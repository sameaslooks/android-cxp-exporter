package io.github.cxpexporter.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
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

private enum class AppScreen { RECORDS, ABOUT, LICENSES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    CxpTheme {
        val vm: MainViewModel = viewModel()
        val state by vm.state.collectAsState()
        val activity = LocalContext.current.findActivity()
        var screen by rememberSaveable { mutableStateOf(AppScreen.RECORDS) }
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
        }
        val title = stringResource(titleRes)
        val backDescription = stringResource(R.string.action_back)
        val clearDescription = stringResource(R.string.action_clear)
        val aboutDescription = stringResource(R.string.action_about)

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
                            IconButton(onClick = { screen = AppScreen.ABOUT }) {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = aboutDescription,
                                )
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
                    contentPadding = padding,
                )
                AppScreen.ABOUT -> AboutScreen(
                    contentPadding = padding,
                    onOpenLicenses = { screen = AppScreen.LICENSES },
                )
                AppScreen.LICENSES -> LicensesScreen(contentPadding = padding)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("No activity found in context")
}
