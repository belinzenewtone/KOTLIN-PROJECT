package com.personal.lifeOS.core.sync

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
            println("SYNC_START[$jobType]")
        }

        override fun onJobSucceeded(jobType: String) {
            println("SYNC_SUCCESS[$jobType]")
        }

        override fun onJobFailed(
            jobType: String,
            message: String?,
        ) {
            println("SYNC_FAIL[$jobType] ${message.orEmpty()}")
        }
    }
