package io.github.cxpexporter.core.cxf

enum class RecordKind { PASSKEY, PASSWORD }

/**
 * One credential extracted from a CXF document.
 * Secrets are redacted in [toString]; call [wipe] to clear the private key.
 */
data class Record(
    val id: String,
    val kind: RecordKind,
    val title: String,
    val username: String,
    val password: String?,
    val urls: List<String>,
    val androidApps: List<String>,
    val accountName: String,
    val created: Long?,
    val modified: Long?,
    val rpId: String? = null,
    val credentialId: String? = null,
    val userHandle: String? = null,
    val userDisplayName: String? = null,
    val privateKeyPkcs8: ByteArray? = null,
) {
    fun wipe() {
        privateKeyPkcs8?.fill(0)
    }

    override fun toString(): String =
        "Record(id=$id, kind=$kind, title=$title, rpId=$rpId)"
}

data class ImportResult(
    val records: List<Record>,
    val skipped: Int,
    val exporterName: String?,
    val rawJson: String,
)
