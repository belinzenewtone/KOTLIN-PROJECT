package com.personal.lifeOS.platform.files

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ExportShareHelper {
    fun createShareIntent(
        context: Context,
        filePath: String,
        mimeType: String,
    ): Intent? {
        val file = File(filePath)
        if (!file.exists()) return null

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
