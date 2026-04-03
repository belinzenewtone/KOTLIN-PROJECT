package com.personal.lifeOS.core.sync

import android.util.Log
import com.personal.lifeOS.core.observability.AppTelemetry
import javax.inject.Inject
import javax.inject.Singleton

interface SyncTelemetry {
    fun onJobStarted(jobType: String)

    fun onJobSucceeded(jobType: String)

    fun onJobFailed(
        jobType: String,
        message: String?,
    )
}

@Singleton
class LogSyncTelemetry
    @Inject
    constructor() : SyncTelemetry {
        override fun onJobStarted(jobType: String) {
            Log.i("SyncTelemetry", "SYNC_START[$jobType]")
            AppTelemetry.trackEvent(
                name = "sync_job_started",
                attributes = mapOf("job_type" to jobType),
            )
        }

        override fun onJobSucceeded(jobType: String) {
            Log.i("SyncTelemetry", "SYNC_SUCCESS[$jobType]")
            AppTelemetry.trackEvent(
                name = "sync_job_succeeded",
                attributes = mapOf("job_type" to jobType),
            )
        }

        override fun onJobFailed(
            jobType: String,
            message: String?,
        ) {
            Log.e("SyncTelemetry", "SYNC_FAIL[$jobType] ${message.orEmpty()}")
            AppTelemetry.trackEvent(
                name = "sync_job_failed",
                attributes =
                    mapOf(
                        "job_type" to jobType,
                        "reason" to message.orEmpty().ifBlank { "unknown" },
                    ),
                captureAsMessage = true,
            )
        }
    }
