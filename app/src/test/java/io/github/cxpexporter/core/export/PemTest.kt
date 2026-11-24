package io.github.cxpexporter.core.export

import org.junit.Assert.assertEquals
import org.junit.Test

class PemTest {

    @Test
    fun wrapsShortKey() {
        val pem = pkcs8ToPem(byteArrayOf(1, 2, 3))
        assertEquals("-----BEGIN PRIVATE KEY-----\nAQID\n-----END PRIVATE KEY-----", pem)
    }

    @Test
    fun wrapsLongKeyInto64CharLines() {
        val pem = pkcs8ToPem(ByteArray(100) { it.toByte() })
        val lines = pem.lines()

        assertEquals("-----BEGIN PRIVATE KEY-----", lines.first())
        assertEquals("-----END PRIVATE KEY-----", lines.last())
        val body = lines.subList(1, lines.size - 1)
        assertEquals(3, body.size) // 100 bytes -> 136 base64 chars
        assertEquals(64, body[0].length)
        assertEquals(64, body[1].length)
        assertEquals(8, body[2].length)
    }
}
