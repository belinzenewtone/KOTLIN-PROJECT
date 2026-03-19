package com.personal.lifeOS.core.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.personal.lifeOS.core.sync.SyncCoordinator
import com.personal.lifeOS.core.sync.model.SyncTrigger
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class CloudSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val syncCoordinator = syncCoordinator()
            syncCoordinator.enqueueDefault(SyncTrigger.PERIODIC_WORK)
            syncCoordinator.runPending(limit = 40)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun syncCoordinator(): SyncCoordinator {
        val entryPoint =
            EntryPointAccessors.fromApplication(
                applicationContext,
                CloudSyncWorkerEntryPoint::class.java,
            )
        return entryPoint.syncCoordinator()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CloudSyncWorkerEntryPoint {
    fun syncCoordinator(): SyncCoordinator
}
