package io.github.cxpexporter.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.cxpexporter.R

private enum class AppScreen { RECORDS, ABOUT, LICENSES, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    CxpTheme {
        val vm: MainViewModel = viewModel()
        val state by vm.state.collectAsState()
        val availableUpdate by vm.availableUpdate.collectAsState()
        val updateCheckEnabled by vm.updateCheckEnabled.collectAsState()
        val updateChecking by vm.updateChecking.collectAsState()
        val activity = LocalContext.current.findActivity()
        var screen by rememberSaveable { mutableStateOf(AppScreen.RECORDS) }
        var menuOpen by remember { mutableStateOf(false) }
        val snackbar = remember { SnackbarHostState() }

        val message = state.message
        LaunchedEffect(message) {
            if (message != null) {
                snackbar.showSnackbar(message)
                vm.messageShown()
            }
        }

        val titleRes = when (screen) {
            AppScreen.RECORDS -> R.string.app_name
            AppScreen.ABOUT -> R.string.about_title
            AppScreen.LICENSES -> R.string.licenses_title
            AppScreen.SETTINGS -> R.string.settings_title
        }
        val title = stringResource(titleRes)
        val backDescription = stringResource(R.string.action_back)
        val clearDescription = stringResource(R.string.action_clear)
        val moreDescription = stringResource(R.string.action_more)
        val settingsLabel = stringResource(R.string.action_settings)
        val aboutLabel = stringResource(R.string.action_about)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        if (screen != AppScreen.RECORDS) {
                            IconButton(
                                onClick = {
                                    screen = if (screen == AppScreen.LICENSES) AppScreen.ABOUT else AppScreen.RECORDS
                                },
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = backDescription,
                                )
                            }
                        }
                    },
                    actions = {
                        if (screen == AppScreen.RECORDS) {
                            if (state.result != null) {
                                IconButton(onClick = { vm.clear() }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = clearDescription,
                                    )
                                }
                            }
                            Box {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(
                                        Icons.Filled.MoreVert,
                                        contentDescription = moreDescription,
                                    )
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text(settingsLabel) },
                                        onClick = {
                                            menuOpen = false
                                            screen = AppScreen.SETTINGS
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(aboutLabel) },
                                        onClick = {
                                            menuOpen = false
                                            screen = AppScreen.ABOUT
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding: PaddingValues ->
            when (screen) {
                AppScreen.RECORDS -> RecordsScreen(
                    state = state,
                    vm = vm,
                    activity = activity,
                    availableUpdate = availableUpdate,
                    contentPadding = padding,
                )
                AppScreen.ABOUT -> AboutScreen(
                    contentPadding = padding,
                    onOpenLicenses = { screen = AppScreen.LICENSES },
                )
                AppScreen.LICENSES -> LicensesScreen(contentPadding = padding)
                AppScreen.SETTINGS -> SettingsScreen(
                    contentPadding = padding,
                    updateCheckEnabled = updateCheckEnabled,
                    checking = updateChecking,
                    onUpdateCheckEnabledChange = { vm.setUpdateCheckEnabled(it) },
                    onCheckNow = { vm.checkNow() },
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("No activity found in context")
}
