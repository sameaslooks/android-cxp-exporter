package io.github.cxpexporter.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class UpdateInfo(val version: String, val url: String)

object UpdateChecker {

    private const val LATEST_URL =
        "https://api.github.com/repos/sameaslooks/android-cxp-exporter/releases/latest"
    private const val REPO_URL = "https://github.com/sameaslooks/android-cxp-exporter"
    private const val TIMEOUT_MS = 10_000

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Returns the newer release, `null` when the current version is up to date (or the remote
     * version is unparsable), or a failure when the request or parsing fails.
     */
    suspend fun fetchLatest(currentVersion: String): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            Result.success(fetch(currentVersion))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetch(currentVersion: String): UpdateInfo? {
        val connection = URL(LATEST_URL).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "cxp-exporter/$currentVersion")

            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
            val body = connection.inputStream.bufferedReader().use { it.readText() }

            val root = json.parseToJsonElement(body).jsonObject
            val tag = root["tag_name"]?.jsonPrimitive?.contentOrNull
                ?: throw IOException("missing tag_name")
            val htmlUrl = root["html_url"]?.jsonPrimitive?.contentOrNull
            val latest = tag.removePrefix("v")
            if (!isNewer(latest, currentVersion)) return null
            // Only ever open a GitHub page from the response; fall back to the repo itself.
            val url = htmlUrl?.takeIf { it.startsWith("https://github.com/") } ?: REPO_URL
            return UpdateInfo(version = latest, url = url)
        } finally {
            connection.disconnect()
        }
    }
}

/**
 * True when [latest] is a higher numeric major.minor.patch than [current].
 * A leading `v` is ignored, missing parts count as 0, and any non-numeric part makes the
 * comparison false.
 */
internal fun isNewer(latest: String, current: String): Boolean {
    val l = parseVersion(latest) ?: return false
    val c = parseVersion(current) ?: return false
    for (i in 0 until 3) {
        if (l[i] != c[i]) return l[i] > c[i]
    }
    return false
}

private fun parseVersion(raw: String): IntArray? {
    val parts = raw.removePrefix("v").split(".")
    val result = IntArray(3)
    for (i in 0 until minOf(3, parts.size)) {
        val part = parts[i]
        if (part.isEmpty() || !part.all { it.isDigit() }) return null
        result[i] = part.toIntOrNull() ?: return null
    }
    return result
}
