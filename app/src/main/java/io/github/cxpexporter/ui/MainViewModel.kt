package io.github.cxpexporter.ui

import android.app.Activity
import android.app.Application
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.cxpexporter.R
import io.github.cxpexporter.core.cxf.CxfParser
import io.github.cxpexporter.core.cxf.ImportResult
import io.github.cxpexporter.core.cxf.Record
import io.github.cxpexporter.core.cxf.RecordKind
import io.github.cxpexporter.core.export.CsvExporter
import io.github.cxpexporter.core.export.KdbxExporter
import io.github.cxpexporter.data.CxpImporter
import io.github.cxpexporter.data.Settings
import io.github.cxpexporter.data.UpdateChecker
import io.github.cxpexporter.data.UpdateInfo
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RecordFilter { ALL, PASSKEYS, PASSWORDS }

data class UiState(
    val result: ImportResult? = null,
    val filter: RecordFilter = RecordFilter.ALL,
    val query: String = "",
    /** Indices into [ImportResult.records]. */
    val selected: Set<Int> = emptySet(),
    val busy: Boolean = false,
    val message: String? = null,
) {
    /** Indices into [ImportResult.records] that pass the current filter and search. */
    val visible: List<Int>
        get() {
            val records = result?.records ?: return emptyList()
            val q = query.trim()
            return records.indices.filter { i ->
                val r = records[i]
                val kindOk = when (filter) {
                    RecordFilter.ALL -> true
                    RecordFilter.PASSKEYS -> r.kind == RecordKind.PASSKEY
                    RecordFilter.PASSWORDS -> r.kind == RecordKind.PASSWORD
                }
                kindOk && (q.isEmpty() || searchFields(r).any { it.contains(q, ignoreCase = true) })
            }
        }
}

private fun searchFields(r: Record): List<String> =
    listOf(r.title.orEmpty(), r.username.orEmpty(), r.rpId.orEmpty()) + r.urls

private const val DAY_MS = 24L * 60 * 60 * 1000

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = Settings(application)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _updateCheckEnabled = MutableStateFlow(settings.updateCheckEnabled)
    val updateCheckEnabled: StateFlow<Boolean> = _updateCheckEnabled.asStateFlow()

    private val _availableUpdate = MutableStateFlow<UpdateInfo?>(null)
    /** Null when there is nothing to show; dismissed state lasts until the next app start. */
    val availableUpdate: StateFlow<UpdateInfo?> = _availableUpdate.asStateFlow()

    private val _updateChecking = MutableStateFlow(false)
    val updateChecking: StateFlow<Boolean> = _updateChecking.asStateFlow()

    init {
        val due = System.currentTimeMillis() - settings.lastCheckMs > DAY_MS
        if (settings.updateCheckEnabled && due) {
            viewModelScope.launch { runUpdateCheck(manual = false) }
        }
    }

    fun setUpdateCheckEnabled(enabled: Boolean) {
        settings.updateCheckEnabled = enabled
        _updateCheckEnabled.value = enabled
    }

    fun dismissUpdate() {
        _availableUpdate.value = null
    }

    /** Ignores the toggle and the 24 h throttle; the result is shown as a snackbar message. */
    fun checkNow() {
        if (_updateChecking.value) return
        viewModelScope.launch { runUpdateCheck(manual = true) }
    }

    private suspend fun runUpdateCheck(manual: Boolean) {
        _updateChecking.value = true
        try {
            val outcome = UpdateChecker.fetchLatest(currentVersionName())
            outcome.onSuccess { info ->
                settings.lastCheckMs = System.currentTimeMillis()
                _availableUpdate.value = info
            }
            if (manual) {
                val app = getApplication<Application>()
                val text = outcome.fold(
                    onSuccess = { info ->
                        if (info == null) {
                            app.getString(R.string.update_up_to_date)
                        } else {
                            app.getString(R.string.update_available_message, info.version)
                        }
                    },
                    onFailure = { app.getString(R.string.update_check_failed) },
                )
                showMessage(text)
            }
        } finally {
            _updateChecking.value = false
        }
    }

    private fun currentVersionName(): String = runCatching {
        val app = getApplication<Application>()
        app.packageManager
            .getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(0))
            .versionName
            .orEmpty()
    }.getOrDefault("")

    fun importFromCxp(activity: Activity) {
        launchBusy("") {
            setResult(CxpImporter.import(activity))
        }
    }

    fun openJson(uri: Uri, contentResolver: ContentResolver) {
        launchBusy("Open failed: ") {
            val json = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw IOException("cannot open file")
            }
            setResult(withContext(Dispatchers.Default) { CxfParser.parse(json, "") })
        }
    }

    fun toggle(index: Int) {
        _state.update { s ->
            s.copy(selected = if (index in s.selected) s.selected - index else s.selected + index)
        }
    }

    fun selectAll() {
        _state.update { it.copy(selected = it.selected + it.visible) }
    }

    fun selectNone() {
        _state.update { it.copy(selected = it.selected - it.visible.toSet()) }
    }

    fun setFilter(filter: RecordFilter) {
        _state.update { it.copy(filter = filter) }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun showMessage(text: String) {
        _state.update { it.copy(message = text) }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }

    fun exportKdbx(uri: Uri, password: String, contentResolver: ContentResolver) {
        launchBusy("Export failed: ") {
            val records = selectedRecords() ?: return@launchBusy
            withContext(Dispatchers.IO) {
                contentResolver.openOutputStream(uri)?.use { KdbxExporter.write(records, password, it) }
                    ?: throw IOException("cannot open file for writing")
            }
            showMessage("Exported ${records.size} records")
        }
    }

    /** Writes the raw CXF JSON as received; it is not filtered by the selection. */
    fun exportJson(uri: Uri, contentResolver: ContentResolver) {
        launchBusy("Export failed: ") {
            val raw = _state.value.result?.rawJson ?: throw IllegalStateException("no records loaded")
            withContext(Dispatchers.IO) {
                contentResolver.openOutputStream(uri)?.use { it.write(raw.toByteArray(Charsets.UTF_8)) }
                    ?: throw IOException("cannot open file for writing")
            }
            showMessage("Exported CXF JSON")
        }
    }

    fun exportCsv(uri: Uri, contentResolver: ContentResolver) {
        launchBusy("Export failed: ") {
            val records = selectedRecords() ?: return@launchBusy
            withContext(Dispatchers.IO) {
                contentResolver.openOutputStream(uri)?.use { CsvExporter.write(records, it) }
                    ?: throw IOException("cannot open file for writing")
            }
            showMessage("Exported ${records.size} records")
        }
    }

    /** Wipes secrets from memory and resets the screen. */
    fun clear() {
        wipeRecords()
        _state.value = UiState()
    }

    override fun onCleared() {
        wipeRecords()
    }

    private fun setResult(result: ImportResult) {
        wipeRecords()
        _state.update {
            it.copy(
                result = result,
                filter = RecordFilter.ALL,
                query = "",
                selected = result.records.indices.toSet(),
                message = null,
            )
        }
    }

    private fun wipeRecords() {
        _state.value.result?.records?.forEach { it.wipe() }
    }

    private fun selectedRecords(): List<Record>? {
        val s = _state.value
        val records = s.result?.records.orEmpty().filterIndexed { i, _ -> i in s.selected }
        if (records.isEmpty()) {
            showMessage("No records selected")
            return null
        }
        return records
    }

    private fun launchBusy(errorPrefix: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showMessage(errorPrefix + CxpImporter.errorMessage(e))
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }
}
