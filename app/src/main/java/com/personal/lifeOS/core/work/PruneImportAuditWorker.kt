package com.personal.lifeOS.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.personal.lifeOS.core.database.dao.ImportAuditDao
import com.personal.lifeOS.core.security.AuthSessionStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Weekly cleanup worker that deletes import audit entries older than 90 days.
 * Prevents unbounded growth of the import_audit table.
 * Registered as a PeriodicWorkRequest with 7-day interval.
 */
@HiltWorker
class PruneImportAuditWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val importAuditDao: ImportAuditDao,
        private val authSessionStore: AuthSessionStore,
    ) : CoroutineWorker(context, workerParams) {

        override suspend fun doWork(): Result {
            return runCatching {
                val userId = authSessionStore.getUserId()
                val cutoffMs = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
                importAuditDao.deleteOlderThan(userId, cutoffMs)
                Result.success()
            }.getOrElse { Result.retry() }
        }
    }
