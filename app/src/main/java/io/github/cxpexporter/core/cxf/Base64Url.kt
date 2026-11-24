package io.github.cxpexporter.core.cxf

import java.util.Base64

object Base64Url {
    /** Accepts url-safe or standard alphabet, with or without padding. */
    fun decode(input: String): ByteArray {
        val normalized = input.trim().replace('-', '+').replace('_', '/').trimEnd('=')
        val padded = when (normalized.length % 4) {
            0 -> normalized
            2 -> "$normalized=="
            3 -> "$normalized="
            else -> throw IllegalArgumentException("Invalid base64 length")
        }
        return Base64.getDecoder().decode(padded)
    }

    fun encodeNoPadding(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
