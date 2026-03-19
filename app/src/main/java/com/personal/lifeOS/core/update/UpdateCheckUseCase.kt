package com.personal.lifeOS.core.update

import android.content.Context
import com.personal.lifeOS.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateCheckUseCase
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val diagnosticsRepository: UpdateDiagnosticsRepository,
    ) {
        suspend operator fun invoke(): AppUpdateInfo? {
            if (BuildConfig.OTA_MANIFEST_URL.isBlank()) return null
            return when (val result = OtaUpdateManager.checkForUpdate(context, BuildConfig.OTA_MANIFEST_URL)) {
                is OtaCheckResult.UpdateAvailable -> {
                    val info =
                        AppUpdateInfo(
                            versionCode = result.manifest.versionCode,
                            versionName = result.manifest.versionName,
                            required = result.manifest.mandatory,
                            downloadUrl = result.manifest.apkUrl,
                            checksumSha256 = result.manifest.apkSha256,
                            checkedAt = System.currentTimeMillis(),
                        )
                    diagnosticsRepository.save(info)
                    info
                }

                else -> null
            }
        }
    }
