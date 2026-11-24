package io.github.cxpexporter.core.cxf

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class Base64UrlTest {

    private val bytes = byteArrayOf(0xFB.toByte(), 0xFF.toByte(), 0xBF.toByte(), 0x01)

    @Test
    fun encodeNoPaddingUsesUrlAlphabet() {
        assertEquals("-_-_AQ", Base64Url.encodeNoPadding(bytes))
    }

    @Test
    fun decodeAcceptsUrlAlphabetWithAndWithoutPadding() {
        assertArrayEquals(bytes, Base64Url.decode("-_-_AQ"))
        assertArrayEquals(bytes, Base64Url.decode("-_-_AQ=="))
        assertArrayEquals(bytes.copyOf(2), Base64Url.decode("+/8="))
        assertArrayEquals(bytes.copyOf(2), Base64Url.decode("-_8"))
    }

    @Test
    fun decodeAcceptsStandardAlphabet() {
        assertArrayEquals(bytes, Base64Url.decode("+/+/AQ=="))
    }

    @Test
    fun roundTripsRandomLengths() {
        for (n in 0..40) {
            val data = ByteArray(n) { (it * 37 + 11).toByte() }
            assertArrayEquals(data, Base64Url.decode(Base64Url.encodeNoPadding(data)))
        }
    }
}
