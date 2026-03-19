package com.personal.lifeOS.core.sync

import com.personal.lifeOS.core.database.entity.SyncJobEntity
import com.personal.lifeOS.core.sync.model.SyncJobType
import kotlinx.coroutines.flow.Flow

interface SyncQueueStore {
    suspend fun enqueue(
        type: SyncJobType,
        entityType: String,
        entityId: String,
        payload: String = "{}",
    ): Long

    suspend fun dueJobs(limit: Int = 50): List<SyncJobEntity>

    suspend fun markSyncing(job: SyncJobEntity)

    suspend fun markSynced(job: SyncJobEntity)

    suspend fun markFailed(
        job: SyncJobEntity,
        error: String?,
        nextRunAt: Long,
    )

    suspend fun pruneSynced(olderThan: Long): Int

    fun observeJobs(): Flow<List<SyncJobEntity>>
}
