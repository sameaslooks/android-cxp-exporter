package io.github.cxpexporter.core.export

import app.keemobile.kotpass.cryptography.EncryptedValue
import app.keemobile.kotpass.database.Credentials
import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.database.decode
import app.keemobile.kotpass.models.Entry
import io.github.cxpexporter.core.cxf.Record
import io.github.cxpexporter.core.cxf.RecordKind
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KdbxExporterTest {

    private val password = "master-pw"

    private val passkey = Record(
        id = "pk",
        kind = RecordKind.PASSKEY,
        title = "example.com",
        username = "alice",
        password = null,
        urls = emptyList(),
        androidApps = emptyList(),
        accountName = "alice@example.com",
        created = 1700000001L,
        modified = 1700000002L,
        rpId = "example.com",
        credentialId = "AQID",
        userHandle = "BAUG",
        userDisplayName = "Alice",
        privateKeyPkcs8 = byteArrayOf(1, 2, 3),
    )

    private val login = Record(
        id = "pw",
        kind = RecordKind.PASSWORD,
        title = "site",
        username = "bob",
        password = "hunter2",
        urls = listOf("https://site.test", "https://site.test/login"),
        androidApps = listOf("com.site.app"),
        accountName = "bob",
        created = null,
        modified = null,
    )

    @Test
    fun roundTripKeepsPasskeyFields() {
        val out = ByteArrayOutputStream()
        KdbxExporter.write(listOf(passkey, login), password, out)

        val decoded = KeePassDatabase.decode(
            ByteArrayInputStream(out.toByteArray()),
            Credentials.from(EncryptedValue.fromString(password)),
        )

        val entries: List<Entry> = decoded.content.group.groups.flatMap { group -> group.entries }
        val passkeyEntry = entries.single { entry -> entry.fields.containsKey("KPEX_PASSKEY_RELYING_PARTY") }
        assertTrue(passkeyEntry.fields.containsKey("KPEX_PASSKEY_CREDENTIAL_ID"))
        assertTrue(passkeyEntry.fields.containsKey("KPEX_PASSKEY_PRIVATE_KEY_PEM"))
        assertTrue(passkeyEntry.fields.containsKey("KPEX_PASSKEY_USER_HANDLE"))
        assertEquals(2, entries.size)
    }
}
