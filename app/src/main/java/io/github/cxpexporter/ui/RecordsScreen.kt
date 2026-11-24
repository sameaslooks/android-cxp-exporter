package io.github.cxpexporter.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.cxpexporter.R
import io.github.cxpexporter.core.cxf.Record
import io.github.cxpexporter.core.cxf.RecordKind
import io.github.cxpexporter.data.PasswordSaver
import io.github.cxpexporter.data.SaveCancelledException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private const val MIME_KDBX = "application/octet-stream"
private const val MIME_JSON = "application/json"
private const val MIME_CSV = "text/csv"

private enum class ExportFormat(@StringRes val label: Int) {
    KDBX(R.string.export_kdbx),
    CXF_JSON(R.string.export_cxf_json),
    KEEPASSXC_CSV(R.string.export_keepassxc_csv),
}

private fun exportFileName(ext: String): String =
    "cxp-export-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".$ext"

@StringRes
private fun filterLabel(filter: RecordFilter): Int = when (filter) {
    RecordFilter.ALL -> R.string.filter_all
    RecordFilter.PASSKEYS -> R.string.filter_passkeys
    RecordFilter.PASSWORDS -> R.string.filter_passwords
}

@Composable
fun RecordsScreen(
    state: UiState,
    vm: MainViewModel,
    activity: Activity,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val scope = rememberCoroutineScope()

    var exportMenu by remember { mutableStateOf(false) }
    var showMasterPassword by remember { mutableStateOf(false) }
    var pendingUnencrypted by remember { mutableStateOf<ExportFormat?>(null) }
    var passkeyInfo by remember { mutableStateOf(false) }
    // Held only between the password dialog and the save-location result; never saved to bundle.
    var kdbxPassword by remember { mutableStateOf<String?>(null) }

    val openJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.openJson(uri, resolver)
    }
    val saveKdbx = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(MIME_KDBX)) { uri ->
        val password = kdbxPassword
        kdbxPassword = null
        if (uri != null && password != null) vm.exportKdbx(uri, password, resolver)
    }
    val saveJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(MIME_JSON)) { uri ->
        if (uri != null) vm.exportJson(uri, resolver)
    }
    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(MIME_CSV)) { uri ->
        if (uri != null) vm.exportCsv(uri, resolver)
    }

    fun startExport(format: ExportFormat) {
        if (format == ExportFormat.KDBX) showMasterPassword = true else pendingUnencrypted = format
    }

    if (showMasterPassword) {
        MasterPasswordDialog(
            onDismiss = { showMasterPassword = false },
            onConfirm = { password ->
                showMasterPassword = false
                kdbxPassword = password
                saveKdbx.launch(exportFileName("kdbx"))
            },
        )
    }

    pendingUnencrypted?.let { format ->
        UnencryptedExportDialog(
            onDismiss = { pendingUnencrypted = null },
            onConfirm = {
                pendingUnencrypted = null
                when (format) {
                    ExportFormat.KDBX -> saveKdbx.launch(exportFileName("kdbx"))
                    ExportFormat.CXF_JSON -> saveJson.launch(exportFileName("json"))
                    ExportFormat.KEEPASSXC_CSV -> saveCsv.launch(exportFileName("csv"))
                }
            },
        )
    }

    if (passkeyInfo) {
        PasskeyInfoDialog(onDismiss = { passkeyInfo = false })
    }

    val result = state.result
    Column(Modifier.padding(contentPadding).fillMaxSize()) {
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())

        if (result == null) {
            EmptyState(
                busy = state.busy,
                onImportCxp = { vm.importFromCxp(activity) },
                onOpenJson = { openJson.launch(arrayOf(MIME_JSON, "*/*")) },
            )
        } else {
            val passkeys = result.records.count { it.kind == RecordKind.PASSKEY }
            val passwords = result.records.size - passkeys
            Text(
                stringResource(R.string.summary, passkeys, passwords, result.skipped, result.exporterName.orEmpty()),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RecordFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { vm.setFilter(filter) },
                        label = { Text(stringResource(filterLabel(filter))) },
                    )
                }
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = { vm.setQuery(it) },
                label = { Text(stringResource(R.string.search_label)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Row(Modifier.padding(horizontal = 8.dp)) {
                TextButton(onClick = { vm.selectAll() }) { Text(stringResource(R.string.action_select_all)) }
                TextButton(onClick = { vm.selectNone() }) { Text(stringResource(R.string.action_select_none)) }
            }

            LazyColumn(Modifier.weight(1f)) {
                items(state.visible, key = { it }) { index ->
                    val record = result.records[index]
                    RecordRow(
                        record = record,
                        selected = index in state.selected,
                        onToggle = { vm.toggle(index) },
                        onSave = {
                            scope.launch {
                                val saveResult = PasswordSaver.save(
                                    activity,
                                    record.username.orEmpty(),
                                    record.password.orEmpty(),
                                )
                                val text = saveResult.fold(
                                    onSuccess = { context.getString(R.string.msg_password_saved) },
                                    onFailure = { e ->
                                        if (e is SaveCancelledException) {
                                            context.getString(R.string.msg_save_cancelled)
                                        } else {
                                            context.getString(R.string.msg_save_failed, e.message ?: e.javaClass.simpleName)
                                        }
                                    },
                                )
                                vm.showMessage(text)
                            }
                        },
                        onPasskeyInfo = { passkeyInfo = true },
                    )
                }
            }

            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Box {
                        Button(
                            onClick = { exportMenu = true },
                            enabled = state.selected.isNotEmpty() && !state.busy,
                        ) {
                            Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_export))
                        }
                        DropdownMenu(expanded = exportMenu, onDismissRequest = { exportMenu = false }) {
                            ExportFormat.entries.forEach { format ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(format.label)) },
                                    onClick = {
                                        exportMenu = false
                                        startExport(format)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(busy: Boolean, onImportCxp: () -> Unit, onOpenJson: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.empty_text), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onImportCxp, enabled = !busy) {
            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_import_cxp))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onOpenJson, enabled = !busy) {
            Text(stringResource(R.string.action_open_json))
        }
    }
}

@Composable
private fun RecordRow(
    record: Record,
    selected: Boolean,
    onToggle: () -> Unit,
    onSave: () -> Unit,
    onPasskeyInfo: () -> Unit,
) {
    val isPasskey = record.kind == RecordKind.PASSKEY
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
        Icon(
            imageVector = if (isPasskey) Icons.Filled.Key else Icons.Filled.Lock,
            contentDescription = stringResource(if (isPasskey) R.string.kind_passkey else R.string.kind_password),
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                record.title.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val username = record.username.orEmpty()
            if (username.isNotEmpty()) {
                Text(username, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val target = if (isPasskey) record.rpId.orEmpty() else record.urls.firstOrNull().orEmpty()
            if (target.isNotEmpty()) {
                Text(
                    target,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (isPasskey) {
            IconButton(onClick = onPasskeyInfo) {
                Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.content_passkey_info))
            }
        } else {
            TextButton(onClick = onSave) { Text(stringResource(R.string.action_save)) }
        }
    }
}
