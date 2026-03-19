package com.personal.lifeOS.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OtaManifestInterpretationTest {
    @Test
    fun `parseManifestBody parses snake-case manifest payload`() {
        val json =
            """
            {
              "version_code": 120,
              "version_name": "2.1.0",
              "apk_url": "https://cdn.example.com/personalos.apk",
              "apk_sha256": "abc123",
              "mandatory": true
            }
            """.trimIndent()

        val manifest = parseManifestBody(json)
        assertNotNull(manifest)
        assertEquals(120L, manifest?.versionCode)
        assertEquals("2.1.0", manifest?.versionName)
        assertEquals("https://cdn.example.com/personalos.apk", manifest?.apkUrl)
        assertEquals("abc123", manifest?.apkSha256)
        assertTrue(manifest?.mandatory == true)
    }

    @Test
    fun `parseManifestBody returns null for malformed payload`() {
        val malformedJson = """{ "version_code": """
        val manifest = parseManifestBody(malformedJson)
        assertNull(manifest)
    }

    @Test
    fun `evaluateManifest returns update available when remote is newer`() {
        val manifest =
            OtaUpdateManifest(
                versionCode = 100L,
                versionName = "1.0.0",
                apkUrl = "https://cdn.example.com/update.apk",
                mandatory = false,
            )

        val result = evaluateManifest(manifest, currentVersionCode = 99L)
        assertTrue(result is OtaCheckResult.UpdateAvailable)
    }

    @Test
    fun `evaluateManifest returns up-to-date when remote is same or lower`() {
        val manifest =
            OtaUpdateManifest(
                versionCode = 100L,
                versionName = "1.0.0",
                apkUrl = "https://cdn.example.com/update.apk",
                mandatory = false,
            )

        val resultEqual = evaluateManifest(manifest, currentVersionCode = 100L)
        val resultLower = evaluateManifest(manifest, currentVersionCode = 101L)

        assertTrue(resultEqual is OtaCheckResult.UpToDate)
        assertTrue(resultLower is OtaCheckResult.UpToDate)
    }

    @Test
    fun `evaluateManifest returns error for invalid fields`() {
        val invalidManifest =
            OtaUpdateManifest(
                versionCode = 0L,
                versionName = null,
                apkUrl = "",
            )

        val result = evaluateManifest(invalidManifest, currentVersionCode = 1L)
        assertTrue(result is OtaCheckResult.Error)
    }
}
