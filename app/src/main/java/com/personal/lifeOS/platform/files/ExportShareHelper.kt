package com.personal.lifeOS.platform.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ExportShareHelper {
    fun createShareIntent(
        context: Context,
        filePath: String,
        mimeType: String,
    ): Intent? {
        return createShareIntent(
            context = context,
            filePath = filePath,
            mimeType = mimeType,
            uriProvider = { sourceContext, sourceFile ->
                runCatching {
                    FileProvider.getUriForFile(
                        sourceContext,
                        "${sourceContext.packageName}.fileprovider",
                        sourceFile,
                    )
                }.getOrNull()
            },
        )
    }

    internal fun createShareIntent(
        context: Context,
        filePath: String,
        mimeType: String,
        uriProvider: (Context, File) -> Uri?,
    ): Intent? {
        val file = File(filePath)
        if (!file.exists()) return null

        val uri = uriProvider(context, file) ?: return null
        return Intent(Intent.ACTION_SEND)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
