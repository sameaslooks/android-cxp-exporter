package io.github.cxpexporter.core.export

import java.util.Base64

/** Wraps a DER-encoded PKCS#8 private key in PEM armor with 64-char lines. */
fun pkcs8ToPem(der: ByteArray): String {
    val body = Base64.getEncoder().encodeToString(der).chunked(64).joinToString("\n")
    return "-----BEGIN PRIVATE KEY-----\n$body\n-----END PRIVATE KEY-----"
}
