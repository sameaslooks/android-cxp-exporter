package io.github.cxpexporter.core.export

import app.keemobile.kotpass.constants.BasicField
import app.keemobile.kotpass.cryptography.EncryptedValue
import app.keemobile.kotpass.database.Credentials
import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.database.encode
import app.keemobile.kotpass.database.modifiers.modifyParentGroup
import app.keemobile.kotpass.models.Entry
import app.keemobile.kotpass.models.EntryFields
import app.keemobile.kotpass.models.EntryValue
import app.keemobile.kotpass.models.Group
import app.keemobile.kotpass.models.Meta
import app.keemobile.kotpass.models.TimeData
import io.github.cxpexporter.core.cxf.Record
import io.github.cxpexporter.core.cxf.RecordKind
import java.io.OutputStream
import java.time.Instant
import java.util.UUID

/** KDBX 4 writer: root "CXP Import" with "Passkeys" and "Passwords" groups. */
object KdbxExporter {
    private const val ROOT_NAME = "CXP Import"

    fun write(records: List<Record>, masterPassword: String, out: OutputStream) {
        val passkeys = records.filter { it.kind == RecordKind.PASSKEY }.map { passkeyEntry(it) }
        val passwords = records.filter { it.kind == RecordKind.PASSWORD }.map { passwordEntry(it) }

        val database = KeePassDatabase.Ver4x.create(
            rootName = ROOT_NAME,
            meta = Meta(generator = "CXP Exporter"),
            credentials = Credentials.from(EncryptedValue.fromString(masterPassword)),
        ).modifyParentGroup {
            copy(
                groups = listOf(
                    Group(uuid = UUID.randomUUID(), name = "Passkeys", entries = passkeys),
                    Group(uuid = UUID.randomUUID(), name = "Passwords", entries = passwords),
                ),
            )
        }

        database.encode(out)
    }

    private fun passwordEntry(r: Record): Entry {
        val fields = mapOf<String, EntryValue>(
            BasicField.Title.key to EntryValue.Plain(r.title),
            BasicField.UserName.key to EntryValue.Plain(r.username),
            BasicField.Password.key to EntryValue.Encrypted(EncryptedValue.fromString(r.password.orEmpty())),
            BasicField.Url.key to EntryValue.Plain(r.urls.firstOrNull().orEmpty()),
            BasicField.Notes.key to EntryValue.Plain(r.extraNotes()),
        )
        return Entry(
            uuid = UUID.randomUUID(),
            fields = EntryFields(fields),
            times = times(r),
        )
    }

    private fun passkeyEntry(r: Record): Entry {
        val fields = linkedMapOf<String, EntryValue>(
            BasicField.Title.key to EntryValue.Plain(r.title),
            BasicField.UserName.key to EntryValue.Plain(r.username),
            BasicField.Password.key to EntryValue.Plain(""),
            BasicField.Url.key to EntryValue.Plain(r.rpId?.let { "https://$it" }.orEmpty()),
            BasicField.Notes.key to EntryValue.Plain(r.extraNotes()),
            "KPEX_PASSKEY_CREDENTIAL_ID" to EntryValue.Plain(r.credentialId.orEmpty()),
            "KPEX_PASSKEY_PRIVATE_KEY_PEM" to EntryValue.Encrypted(
                EncryptedValue.fromString(r.privateKeyPkcs8?.let { pkcs8ToPem(it) }.orEmpty()),
            ),
            "KPEX_PASSKEY_RELYING_PARTY" to EntryValue.Plain(r.rpId.orEmpty()),
            "KPEX_PASSKEY_USERNAME" to EntryValue.Plain(r.username),
            "KPEX_PASSKEY_USER_HANDLE" to EntryValue.Plain(r.userHandle.orEmpty()),
        )
        return Entry(
            uuid = UUID.randomUUID(),
            fields = EntryFields(fields),
            tags = listOf("Passkey"),
            times = times(r),
        )
    }

    private fun times(r: Record): TimeData? {
        if (r.created == null && r.modified == null) return null
        return TimeData(
            creationTime = r.created?.let { Instant.ofEpochSecond(it) },
            lastAccessTime = null,
            lastModificationTime = r.modified?.let { Instant.ofEpochSecond(it) },
            locationChanged = null,
            expiryTime = null,
        )
    }
}
