package io.github.cxpexporter.core.export

import io.github.cxpexporter.core.cxf.Record
import io.github.cxpexporter.core.cxf.RecordKind
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CsvExporterTest {

    private fun password(
        title: String,
        username: String,
        password: String?,
        urls: List<String> = emptyList(),
    ) = Record(
        id = title,
        kind = RecordKind.PASSWORD,
        title = title,
        username = username,
        password = password,
        urls = urls,
        androidApps = emptyList(),
        accountName = "acc",
        created = 1700000000L,
        modified = null,
    )

    private fun export(records: List<Record>): String {
        val out = ByteArrayOutputStream()
        CsvExporter.write(records, out)
        return out.toString(Charsets.UTF_8.name())
    }

    @Test
    fun writesHeaderAndCrlfLines() {
        val text = export(listOf(password("site", "user", "pw", listOf("https://site.test"))))
        val lines = text.split("\r\n")

        assertEquals(
            "\"Group\",\"Title\",\"Username\",\"Password\",\"URL\",\"Notes\"," +
                "\"TOTP\",\"Icon\",\"Last Modified\",\"Created\"",
            lines[0],
        )
        assertEquals(
            "\"Root/CXP Import\",\"site\",\"user\",\"pw\",\"https://site.test\",\"\"," +
                "\"\",\"\",\"\",\"2023-11-14T22:13:20Z\"",
            lines[1],
        )
        assertEquals("", lines[2])
    }

    @Test
    fun quotesCommasQuotesAndNewlines() {
        val text = export(listOf(password("a, \"b\"", "line1\nline2", "p,w")))
        assertEquals(true, text.contains("\"a, \"\"b\"\"\""))
        assertEquals(true, text.contains("\"line1\nline2\""))
        assertEquals(true, text.contains("\"p,w\""))
    }

    @Test
    fun excludesPasskeys() {
        val passkey = Record(
            id = "pk",
            kind = RecordKind.PASSKEY,
            title = "pk",
            username = "u",
            password = null,
            urls = emptyList(),
            androidApps = emptyList(),
            accountName = "",
            created = null,
            modified = null,
            rpId = "example.com",
        )
        val text = export(listOf(passkey))
        assertEquals(1, text.split("\r\n").count { it.isNotEmpty() })
        assertFalse(text.contains("pk"))
    }
}
