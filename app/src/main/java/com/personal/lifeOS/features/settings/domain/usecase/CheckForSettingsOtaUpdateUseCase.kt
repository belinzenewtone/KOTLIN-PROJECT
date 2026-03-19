package com.personal.lifeOS.features.settings.domain.usecase

import android.content.Context
import com.personal.lifeOS.BuildConfig
import com.personal.lifeOS.core.update.OtaCheckResult
import com.personal.lifeOS.core.update.OtaUpdateManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CheckForSettingsOtaUpdateUseCase
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        suspend operator fun invoke(): OtaCheckResult {
            return OtaUpdateManager.checkForUpdate(context, BuildConfig.OTA_MANIFEST_URL)
        }
    }
