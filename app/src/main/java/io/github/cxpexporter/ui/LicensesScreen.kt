package io.github.cxpexporter.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.cxpexporter.R

private const val APACHE_LICENSE_ASSET = "licenses/APACHE-2.0.txt"

/**
 * One third-party component. Apache 2.0 entries expand inline to show the bundled license text;
 * other entries only link to their license.
 */
private data class ThirdPartyLibrary(
    val name: String,
    val copyright: String,
    val license: String,
    val projectUrl: String,
    val showsLicenseText: Boolean = false,
)

private val thirdPartyLibraries = listOf(
    ThirdPartyLibrary(
        name = "AndroidX (Core, Activity, Lifecycle, Credentials, Credentials provider events)",
        copyright = "The Android Open Source Project",
        license = "Apache License 2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx",
        showsLicenseText = true,
    ),
    ThirdPartyLibrary(
        name = "Jetpack Compose and Material 3",
        copyright = "The Android Open Source Project",
        license = "Apache License 2.0",
        projectUrl = "https://developer.android.com/jetpack/compose",
        showsLicenseText = true,
    ),
    ThirdPartyLibrary(
        name = "Kotlin standard library",
        copyright = "JetBrains s.r.o. and Kotlin Programming Language contributors",
        license = "Apache License 2.0",
        projectUrl = "https://github.com/JetBrains/kotlin",
        showsLicenseText = true,
    ),
    ThirdPartyLibrary(
        name = "kotlinx.coroutines",
        copyright = "JetBrains s.r.o. and contributors",
        license = "Apache License 2.0",
        projectUrl = "https://github.com/Kotlin/kotlinx.coroutines",
        showsLicenseText = true,
    ),
    ThirdPartyLibrary(
        name = "kotlinx.serialization",
        copyright = "JetBrains s.r.o. and contributors",
        license = "Apache License 2.0",
        projectUrl = "https://github.com/Kotlin/kotlinx.serialization",
        showsLicenseText = true,
    ),
    ThirdPartyLibrary(
        name = "Google Play services credentials libraries (credentials-play-services-auth, providerevents-play-services)",
        copyright = "Google LLC",
        license = "Android Software Development Kit License",
        projectUrl = "https://developer.android.com/studio/terms",
    ),
    ThirdPartyLibrary(
        name = "kotpass",
        copyright = "keemobile and contributors",
        license = "Apache License 2.0",
        projectUrl = "https://github.com/keemobile/kotpass",
        showsLicenseText = true,
    ),
    ThirdPartyLibrary(
        name = "Bouncy Castle (transitive dependency of kotpass)",
        copyright = "The Legion of the Bouncy Castle Inc.",
        license = "Bouncy Castle License (MIT-style)",
        projectUrl = "https://www.bouncycastle.org/licence.html",
    ),
)

@Composable
fun LicensesScreen(
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val apacheText = remember(context) { readAsset(context, APACHE_LICENSE_ASSET) }
    val intro = stringResource(R.string.licenses_intro)
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = intro,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        items(thirdPartyLibraries, key = { it.name }) { library ->
            LibraryItem(
                library = library,
                expanded = expanded == library.name,
                licenseText = apacheText,
                onToggle = { expanded = if (expanded == library.name) null else library.name },
                onOpenUrl = { url -> uriHandler.openUri(url) },
            )
        }
    }
}

@Composable
private fun LibraryItem(
    library: ThirdPartyLibrary,
    expanded: Boolean,
    licenseText: String,
    onToggle: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val monospace = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
    val showLicense = stringResource(R.string.licenses_show)
    val hideLicense = stringResource(R.string.licenses_hide)
    val projectPage = stringResource(R.string.licenses_project_page)
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = library.showsLicenseText, onClick = onToggle)
                .padding(vertical = 4.dp),
        ) {
            Text(library.name, style = MaterialTheme.typography.titleSmall)
            Text(library.copyright, style = MaterialTheme.typography.bodySmall)
            Text(library.license, style = MaterialTheme.typography.bodySmall)
            if (library.showsLicenseText) {
                Text(
                    text = if (expanded) hideLicense else showLicense,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (expanded && library.showsLicenseText) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(text = licenseText, style = monospace)
            }
        }
        TextButton(onClick = { onOpenUrl(library.projectUrl) }) {
            Text(projectPage)
        }
    }
}

private fun readAsset(context: Context, path: String): String =
    runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }
        .getOrElse { "Could not load $path" }
