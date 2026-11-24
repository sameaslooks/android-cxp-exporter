package io.github.cxpexporter.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.cxpexporter.R
import java.security.MessageDigest

@Composable
fun AboutScreen(
    contentPadding: PaddingValues,
    onOpenLicenses: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val appName = stringResource(R.string.app_name)
    val authorUrl = stringResource(R.string.about_author_url)
    val repoUrl = stringResource(R.string.about_repo_url)
    val unavailable = stringResource(R.string.about_signing_unavailable)
    val versionName = remember(context) {
        context.packageManager
            .getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            .versionName
            .orEmpty()
    }
    val certFingerprint = remember(context) { signingCertSha256(context) ?: unavailable }

    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = appName,
            style = MaterialTheme.typography.titleLarge,
        )
        Column {
            Text(stringResource(R.string.about_version, versionName), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.about_package, context.packageName),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { uriHandler.openUri(authorUrl) }
                .padding(vertical = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.about_author),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { uriHandler.openUri(repoUrl) }
                .padding(vertical = 4.dp),
        ) {
            Text(stringResource(R.string.about_source_label), style = MaterialTheme.typography.labelMedium)
            Text(
                text = stringResource(R.string.about_repo_display),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        OutlinedButton(onClick = onOpenLicenses) {
            Text(stringResource(R.string.about_licenses))
        }
        Column {
            Text(stringResource(R.string.about_signing_label), style = MaterialTheme.typography.labelMedium)
            SelectionContainer {
                Text(
                    text = certFingerprint,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
            Text(
                text = stringResource(R.string.about_signing_note),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = stringResource(R.string.about_disclaimer),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun signingCertSha256(context: Context): String? = runCatching {
    val pm = context.packageManager
    val signingInfo = pm.getPackageInfo(
        context.packageName,
        PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
    ).signingInfo
    signingInfo?.apkContentsSigners?.firstOrNull()?.let { sha256Hex(it.toByteArray()) }
}.getOrNull()

private fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString(":") { "%02X".format(it.toInt() and 0xff) }
