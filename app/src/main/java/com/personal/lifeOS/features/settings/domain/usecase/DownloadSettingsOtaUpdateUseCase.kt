package com.personal.lifeOS.features.settings.domain.usecase

import android.content.Context
import com.personal.lifeOS.core.update.OtaDownloadResult
import com.personal.lifeOS.core.update.OtaUpdateManager
import com.personal.lifeOS.core.update.OtaUpdateManifest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DownloadSettingsOtaUpdateUseCase
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        suspend operator fun invoke(
            manifest: OtaUpdateManifest,
            onProgress: (Int?) -> Unit,
        ): OtaDownloadResult {
            return OtaUpdateManager.downloadUpdate(
                context = context,
                manifest = manifest,
                onProgress = onProgress,
            )
        }
    }
