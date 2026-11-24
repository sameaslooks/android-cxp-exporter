package io.github.cxpexporter.data

import android.app.Activity
import androidx.credentials.providerevents.ProviderEventsManager
import androidx.credentials.providerevents.exception.ImportCredentialsCancellationException
import androidx.credentials.providerevents.exception.ImportCredentialsNoExportOptionException
import androidx.credentials.providerevents.exception.ImportCredentialsProviderConfigurationException
import androidx.credentials.providerevents.transfer.CredentialTypes
import androidx.credentials.providerevents.transfer.ImportCredentialsRequest
import androidx.credentials.providerevents.transfer.KnownExtensions
import androidx.credentials.providerevents.transfer.ProviderImportCredentialsResponse
import io.github.cxpexporter.core.cxf.CxfParser
import io.github.cxpexporter.core.cxf.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CxpImporter {

    /** Requests credentials from the CXP exporter chosen by the system and parses the CXF JSON. */
    suspend fun import(activity: Activity): ImportResult {
        val response = requestCredentials(activity)
        val json = response.response.responseJson
        val exporterPackage = response.callingAppInfo.packageName
        return withContext(Dispatchers.Default) { CxfParser.parse(json, exporterPackage) }
    }

    private suspend fun requestCredentials(activity: Activity): ProviderImportCredentialsResponse =
        ProviderEventsManager.create(activity).importCredentials(
            activity,
            ImportCredentialsRequest(
                credentialTypes = setOf(
                    CredentialTypes.CREDENTIAL_TYPE_PUBLIC_KEY,
                    CredentialTypes.CREDENTIAL_TYPE_BASIC_AUTH,
                ),
                knownExtensions = setOf(KnownExtensions.KNOWN_EXTENSION_SHARED),
            ),
        )

    fun errorMessage(e: Throwable): String = when (e) {
        is ImportCredentialsCancellationException -> "Import cancelled"
        is ImportCredentialsNoExportOptionException ->
            "No app offers credentials to export (update Google Play services / Password Manager)"
        is ImportCredentialsProviderConfigurationException -> "This device does not support credential transfer"
        else -> "${e.javaClass.simpleName}: ${e.message}"
    }
}
