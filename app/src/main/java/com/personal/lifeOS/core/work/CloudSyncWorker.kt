package com.personal.lifeOS.core.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.personal.lifeOS.core.utils.CloudSyncService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class CloudSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val cloudSyncService = cloudSyncService()

        val push = cloudSyncService.pushToCloud()
        if (!push.success) {
            if (isTerminalSkip(push.message)) return Result.success()
            return Result.retry()
        }

        val pull = cloudSyncService.pullFromCloud()
        if (!pull.success) {
            if (isTerminalSkip(pull.message)) return Result.success()
            return Result.retry()
        }

        return Result.success()
    }

    private fun cloudSyncService(): CloudSyncService {
        val entryPoint =
            EntryPointAccessors.fromApplication(
                applicationContext,
                CloudSyncWorkerEntryPoint::class.java,
            )
        return entryPoint.cloudSyncService()
    }

    private fun isTerminalSkip(message: String): Boolean {
        return message.contains("Sign in required", ignoreCase = true) ||
            message.contains("Supabase not configured", ignoreCase = true)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CloudSyncWorkerEntryPoint {
    fun cloudSyncService(): CloudSyncService
}
