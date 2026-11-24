package io.github.cxpexporter.core.export

import io.github.cxpexporter.core.cxf.Record
import io.github.cxpexporter.core.cxf.RecordKind
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.time.Instant

/** KeePassXC CSV: passwords only, RFC 4180 quoting, CRLF line endings. The stream is flushed, not closed. */
object CsvExporter {
    private const val GROUP = "Root/CXP Import"
    private val HEADER = listOf(
        "Group", "Title", "Username", "Password", "URL", "Notes",
        "TOTP", "Icon", "Last Modified", "Created",
    )

    fun write(records: List<Record>, out: OutputStream) {
        val sb = StringBuilder()
        sb.append(line(HEADER))
        records.asSequence()
            .filter { it.kind == RecordKind.PASSWORD }
            .forEach { r ->
                sb.append(
                    line(
                        listOf(
                            GROUP,
                            r.title,
                            r.username,
                            r.password.orEmpty(),
                            r.urls.firstOrNull().orEmpty(),
                            r.extraNotes(),
                            "",
                            "",
                            r.modified?.let { Instant.ofEpochSecond(it).toString() }.orEmpty(),
                            r.created?.let { Instant.ofEpochSecond(it).toString() }.orEmpty(),
                        ),
                    ),
                )
            }
        val writer = OutputStreamWriter(out, Charsets.UTF_8)
        writer.write(sb.toString())
        writer.flush()
    }

    private fun line(fields: List<String>): String =
        fields.joinToString(",") { quote(it) } + "\r\n"

    private fun quote(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
}

/** Notes shared by CSV and KDBX: extra URLs (beyond the first) and Android apps. */
internal fun Record.extraNotes(): String {
    val lines = mutableListOf<String>()
    lines += urls.drop(1)
    if (androidApps.isNotEmpty()) lines += "Android apps: " + androidApps.joinToString(", ")
    return lines.joinToString("\n")
}
