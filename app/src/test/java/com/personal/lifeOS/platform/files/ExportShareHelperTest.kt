package com.personal.lifeOS.platform.files

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ExportShareHelperTest {
    @Test
    fun `create share intent returns null for missing file`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val missingFile = File(context.filesDir, "exports/missing-export.json")
        var resolverInvocationCount = 0

        val shareIntent =
            ExportShareHelper.createShareIntent(
                context = context,
                filePath = missingFile.absolutePath,
                mimeType = "application/json",
                uriProvider = { _, _ ->
                    resolverInvocationCount++
                    Uri.parse("content://test/missing")
                },
            )

        assertNull(shareIntent)
        assertEquals(0, resolverInvocationCount)
    }

    @Test
    fun `create share intent builds ACTION_SEND intent for existing file`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val exportFile = File(exportDir, "latest-export.json").apply { writeText("{\"ok\":true}") }
        val expectedUri = Uri.parse("content://com.personal.lifeOS.fileprovider/export/latest-export.json")

        val shareIntent =
            ExportShareHelper.createShareIntent(
                context = context,
                filePath = exportFile.absolutePath,
                mimeType = "application/json",
                uriProvider = { _, _ -> expectedUri },
            )

        assertNotNull(shareIntent)
        val intent = requireNotNull(shareIntent)
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("application/json", intent.type)
        assertTrue((intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)

        val streamUri = intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
        assertNotNull(streamUri)
        assertEquals(expectedUri, requireNotNull(streamUri))
    }

    @Test
    fun `create share intent returns null when uri resolution fails`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val exportFile = File(exportDir, "latest-export.csv").apply { writeText("id,amount\n1,100") }

        val shareIntent =
            ExportShareHelper.createShareIntent(
                context = context,
                filePath = exportFile.absolutePath,
                mimeType = "text/csv",
                uriProvider = { _, _ -> null },
            )

        assertNull(shareIntent)
    }
}
