package io.github.cxpexporter.core.cxf

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CxfParserTest {

    private val sample = """
        {
          "version": "1.0",
          "exporterRpId": "passwords.google.com",
          "exporterDisplayName": "Google Password Manager",
          "timestamp": 1700000000,
          "accounts": [
            {
              "id": "acc1",
              "username": "alice@example.com",
              "email": "alice@example.com",
              "items": [
                {
                  "id": "item1",
                  "title": "example.com",
                  "creationAt": 1700000001,
                  "modifiedAt": 1700000002,
                  "scope": {
                    "urls": ["https://example.com"],
                    "androidApps": [{"bundleId": "com.example.app"}]
                  },
                  "credentials": [
                    {
                      "type": "passkey",
                      "id": "c1",
                      "credentialId": "AQID",
                      "rpId": "example.com",
                      "username": "alice",
                      "userDisplayName": "Alice",
                      "userHandle": "BAUG",
                      "key": "AQID"
                    },
                    {
                      "type": "basic-auth",
                      "id": "c2",
                      "username": {"fieldType": "username", "value": "alice"},
                      "password": {"fieldType": "password", "value": "s3cret"}
                    }
                  ]
                },
                {
                  "id": "item2",
                  "title": "",
                  "scope": {"urls": []},
                  "credentials": [{"type": "note", "id": "c3"}]
                },
                {
                  "id": "item3",
                  "title": "broken"
                }
              ]
            }
          ]
        }
    """.trimIndent()

    @Test
    fun parsesSampleCounts() {
        val result = CxfParser.parse(sample, "com.google.android.gms")

        assertEquals(2, result.records.size)
        assertEquals(2, result.skipped) // 'note' credential + item without credentials
        assertEquals("Google Password Manager", result.exporterName)
        assertEquals(sample, result.rawJson)
    }

    @Test
    fun parsesPasskeyFields() {
        val passkey = CxfParser.parse(sample, null).records.single { it.kind == RecordKind.PASSKEY }

        assertEquals("example.com", passkey.title)
        assertEquals("example.com", passkey.rpId)
        assertEquals("alice", passkey.username)
        assertEquals("Alice", passkey.userDisplayName)
        assertEquals("AQID", passkey.credentialId)
        assertEquals("BAUG", passkey.userHandle)
        assertArrayEquals(byteArrayOf(1, 2, 3), passkey.privateKeyPkcs8)
        assertNull(passkey.password)
        assertEquals("alice@example.com", passkey.accountName)
        assertEquals(1700000001L, passkey.created)
        assertEquals(1700000002L, passkey.modified)
    }

    @Test
    fun parsesBasicAuthFields() {
        val password = CxfParser.parse(sample, null).records.single { it.kind == RecordKind.PASSWORD }

        assertEquals("alice", password.username)
        assertEquals("s3cret", password.password)
        assertEquals(listOf("https://example.com"), password.urls)
        assertEquals(listOf("com.example.app"), password.androidApps)
    }

    @Test
    fun exporterNameFallsBackToRpIdThenPackage() {
        val json = """{"exporterRpId":"passwords.google.com","accounts":[]}"""
        assertEquals("passwords.google.com", CxfParser.parse(json, "pkg").exporterName)

        assertEquals("pkg", CxfParser.parse("{}", "pkg").exporterName)
        assertNull(CxfParser.parse("{}", null).exporterName)
    }

    @Test
    fun titleFallsBackToRpIdAndUrlHost() {
        val json = """
            {"accounts":[{"items":[
              {"title":"","scope":{"urls":["https://www.example.org/login"]},
               "credentials":[{"type":"basic-auth","username":"u","password":"p"}]},
              {"title":"","credentials":[{"type":"passkey","rpId":"rp.test",
               "credentialId":"AQID","key":"AQID"}]},
              {"credentials":[{"type":"basic-auth","username":"u","password":"p"}]}
            ]}]}
        """.trimIndent()

        val records = CxfParser.parse(json, null).records
        assertEquals("www.example.org", records[0].title)
        assertEquals("rp.test", records[1].title)
        assertEquals("Untitled", records[2].title)
    }

    @Test
    fun plainStringEditableFieldIsAccepted() {
        val json = """
            {"accounts":[{"items":[{"title":"t","credentials":[
              {"type":"basic-auth","username":"u","password":"p"}]}]}]}
        """.trimIndent()

        val result = CxfParser.parse(json, null)
        assertEquals(0, result.skipped)
        assertEquals("p", result.records.single().password)
    }

    @Test
    fun malformedCredentialIsSkippedNotFatal() {
        val json = """
            {"accounts":[{"items":[{"title":"t","credentials":[
              {"type":"passkey","rpId":"rp","credentialId":"!!!","key":"AQID"},
              {"type":"basic-auth","username":"u","password":"p"}]}]}]}
        """.trimIndent()

        val result = CxfParser.parse(json, null)
        assertEquals(1, result.records.size)
        assertEquals(1, result.skipped)
    }

    @Test
    fun recordWipeZeroesPrivateKey() {
        val record = CxfParser.parse(sample, null).records.single { it.kind == RecordKind.PASSKEY }
        record.wipe()
        assertArrayEquals(byteArrayOf(0, 0, 0), record.privateKeyPkcs8)
    }
}
