package com.personal.lifeOS.core.update.presentation

import androidx.compose.runtime.saveable.listSaver

internal data class OtaPromptUiState(
    val hasCheckedThisSession: Boolean = false,
    val skippedVersionCode: Long = -1L,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadPercent: Int? = null,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val downloadSpeedBytesPerSec: Long? = null,
    val statusMessage: String? = null,
    val activeDownloadId: Long = -1L,
    val showDialog: Boolean = false,
) {
    fun dismissForVersion(versionCode: Long): OtaPromptUiState {
        return copy(
            skippedVersionCode = versionCode,
            showDialog = false,
        )
    }

    companion object {
        val Saver =
            listSaver<OtaPromptUiState, Any?>(
                save = {
                    listOf(
                        it.hasCheckedThisSession,
                        it.skippedVersionCode,
                        it.isChecking,
                        it.isDownloading,
                        it.downloadPercent,
                        it.downloadedBytes,
                        it.totalBytes,
                        it.downloadSpeedBytesPerSec,
                        it.statusMessage,
                        it.activeDownloadId,
                        it.showDialog,
                    )
                },
                restore = {
                    OtaPromptUiState(
                        hasCheckedThisSession = it[0] as Boolean,
                        skippedVersionCode = it[1] as Long,
                        isChecking = it[2] as Boolean,
                        isDownloading = it[3] as Boolean,
                        downloadPercent = it[4] as Int?,
                        downloadedBytes = it[5] as Long,
                        totalBytes = it[6] as Long?,
                        downloadSpeedBytesPerSec = it[7] as Long?,
                        statusMessage = it[8] as String?,
                        activeDownloadId = it[9] as Long,
                        showDialog = it[10] as Boolean,
                    )
                },
            )
    }
}
