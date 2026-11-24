package io.github.cxpexporter.core.cxf

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import java.net.URI

object CxfParser {
    private val codec = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Throws on invalid JSON or a non-object root; malformed items are counted in [ImportResult.skipped]. */
    fun parse(json: String, exporterPackage: String?): ImportResult {
        val root = codec.parseToJsonElement(json) as? JsonObject
            ?: throw IllegalArgumentException("CXF root is not an object")

        val exporterName = listOf(
            root.text("exporterDisplayName"),
            root.text("exporterRpId"),
            exporterPackage,
        ).firstOrNull { !it.isNullOrBlank() }

        val records = mutableListOf<Record>()
        var skipped = 0

        val accounts = root.array("accounts").orEmpty()
        accounts.forEachIndexed { ai, accountEl ->
            val account = accountEl as? JsonObject
            val items = account?.array("items")
            if (account == null || items == null) {
                skipped++
                return@forEachIndexed
            }
            val accountName = account.text("username")?.takeIf { it.isNotBlank() }
                ?: account.text("email").orEmpty()

            items.forEachIndexed { ii, itemEl ->
                val item = itemEl as? JsonObject
                val credentials = item?.array("credentials")
                if (item == null || credentials == null) {
                    skipped++
                    return@forEachIndexed
                }
                val scope = item.obj("scope")
                val urls = scope?.array("urls").orEmpty().mapNotNull { it.asText() }
                val androidApps = scope?.array("androidApps").orEmpty()
                    .mapNotNull { (it as? JsonObject)?.text("bundleId") }
                val itemTitle = item.text("title")?.takeIf { it.isNotBlank() }
                val created = item.epoch("creationAt")
                val modified = item.epoch("modifiedAt")

                credentials.forEachIndexed { ci, credEl ->
                    val id = "a$ai.i$ii.c$ci"
                    val record = try {
                        parseCredential(
                            id = id,
                            credential = credEl as? JsonObject,
                            itemTitle = itemTitle,
                            urls = urls,
                            androidApps = androidApps,
                            accountName = accountName,
                            created = created,
                            modified = modified,
                        )
                    } catch (e: Exception) {
                        null
                    }
                    if (record == null) skipped++ else records += record
                }
            }
        }

        return ImportResult(
            records = records,
            skipped = skipped,
            exporterName = exporterName,
            rawJson = json,
        )
    }

    private fun parseCredential(
        id: String,
        credential: JsonObject?,
        itemTitle: String?,
        urls: List<String>,
        androidApps: List<String>,
        accountName: String,
        created: Long?,
        modified: Long?,
    ): Record? {
        val c = credential ?: return null
        val host = urls.firstNotNullOfOrNull { hostOf(it) }

        return when (c.text("type")) {
            "passkey" -> {
                val rpId = c.field("rpId")?.takeIf { it.isNotBlank() } ?: return null
                val credentialId = c.field("credentialId") ?: return null
                val key = c.field("key") ?: return null
                val userHandle = c.field("userHandle")?.let {
                    Base64Url.encodeNoPadding(Base64Url.decode(it))
                }
                Record(
                    id = id,
                    kind = RecordKind.PASSKEY,
                    title = itemTitle ?: rpId ?: host ?: "Untitled",
                    username = c.field("username").orEmpty(),
                    password = null,
                    urls = urls,
                    androidApps = androidApps,
                    accountName = accountName,
                    created = created,
                    modified = modified,
                    rpId = rpId,
                    credentialId = Base64Url.encodeNoPadding(Base64Url.decode(credentialId)),
                    userHandle = userHandle,
                    userDisplayName = c.field("userDisplayName"),
                    privateKeyPkcs8 = Base64Url.decode(key),
                )
            }
            "basic-auth" -> {
                val password = c.field("password") ?: return null
                Record(
                    id = id,
                    kind = RecordKind.PASSWORD,
                    title = itemTitle ?: host ?: "Untitled",
                    username = c.field("username").orEmpty(),
                    password = password,
                    urls = urls,
                    androidApps = androidApps,
                    accountName = accountName,
                    created = created,
                    modified = modified,
                )
            }
            else -> null
        }
    }

    private fun hostOf(url: String): String? =
        try {
            URI(url).host?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.array(key: String): JsonArray? = this[key] as? JsonArray

    private fun JsonObject.text(key: String): String? = this[key].asText()

    /** EditableField may be a plain string or an object with a "value" member. */
    private fun JsonObject.field(key: String): String? {
        val el = this[key] ?: return null
        return if (el is JsonObject) el["value"].asText() else el.asText()
    }

    private fun JsonObject.epoch(key: String): Long? =
        (this[key] as? JsonPrimitive)?.doubleOrNull?.toLong()

    private fun JsonElement?.asText(): String? =
        (this as? JsonPrimitive)?.contentOrNull
}
