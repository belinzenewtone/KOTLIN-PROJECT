package com.personal.lifeOS.core.sync

import com.personal.lifeOS.core.database.dao.SyncJobDao
import com.personal.lifeOS.core.database.entity.SyncJobEntity
import com.personal.lifeOS.core.sync.model.SyncJobType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomSyncQueueStore
    @Inject
    constructor(
        private val syncJobDao: SyncJobDao,
    ) : SyncQueueStore {
        override suspend fun enqueue(
            type: SyncJobType,
            entityType: String,
            entityId: String,
            payload: String,
        ): Long {
            val now = System.currentTimeMillis()
            val existing =
                syncJobDao.findActiveJob(
                    jobType = type.name,
                    entityType = entityType,
                    entityId = entityId,
                )

            if (existing != null) {
                syncJobDao.update(
                    existing.copy(
                        payload = payload,
                        status = "QUEUED",
                        nextRunAt = now,
                        updatedAt = now,
                        attemptCount = 0,
                        lastError = null,
                    ),
                )
                return existing.id
            }

            return syncJobDao.insert(
                SyncJobEntity(
                    jobType = type.name,
                    entityType = entityType,
                    entityId = entityId,
                    payload = payload,
                    status = "QUEUED",
                    nextRunAt = now,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }

        override suspend fun dueJobs(limit: Int): List<SyncJobEntity> = syncJobDao.getDueJobs(limit = limit)

        override suspend fun markSyncing(job: SyncJobEntity) {
            syncJobDao.update(
                job.copy(
                    status = "SYNCING",
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }

        override suspend fun markSynced(job: SyncJobEntity) {
            syncJobDao.update(
                job.copy(
                    status = "SYNCED",
                    updatedAt = System.currentTimeMillis(),
                    attemptCount = job.attemptCount + 1,
                    lastError = null,
                ),
            )
        }

        override suspend fun markFailed(
            job: SyncJobEntity,
            error: String?,
            nextRunAt: Long,
        ) {
            syncJobDao.update(
                job.copy(
                    status = "FAILED",
                    updatedAt = System.currentTimeMillis(),
                    attemptCount = job.attemptCount + 1,
                    lastError = error,
                    nextRunAt = nextRunAt,
                ),
            )
        }

        override suspend fun pruneSynced(olderThan: Long): Int = syncJobDao.pruneSynced(olderThan)

        override fun observeJobs(): Flow<List<SyncJobEntity>> = syncJobDao.observeAll()
    }
