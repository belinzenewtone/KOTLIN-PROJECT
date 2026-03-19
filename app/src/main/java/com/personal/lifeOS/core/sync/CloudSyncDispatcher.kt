package com.personal.lifeOS.core.sync

import com.personal.lifeOS.core.database.entity.SyncJobEntity
import com.personal.lifeOS.core.sync.model.SyncJobType
import com.personal.lifeOS.core.utils.CloudSyncService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncDispatcher
    @Inject
    constructor(
        private val cloudSyncService: CloudSyncService,
    ) : SyncDispatcher {
        override suspend fun dispatch(job: SyncJobEntity): Result<Unit> {
            val type = runCatching { SyncJobType.valueOf(job.jobType) }.getOrNull()
            return when (type) {
                SyncJobType.PUSH_ALL -> {
                    val result = cloudSyncService.pushToCloud()
                    if (result.success) Result.success(Unit) else Result.failure(IllegalStateException(result.message))
                }

                SyncJobType.PULL_ALL -> {
                    val result = cloudSyncService.pullFromCloud()
                    if (result.success) Result.success(Unit) else Result.failure(IllegalStateException(result.message))
                }

                SyncJobType.REPAIR_ALL -> {
                    val push = cloudSyncService.pushToCloud()
                    if (!push.success) return Result.failure(IllegalStateException(push.message))
                    val pull = cloudSyncService.pullFromCloud()
                    if (pull.success) Result.success(Unit) else Result.failure(IllegalStateException(pull.message))
                }

                null -> Result.failure(IllegalArgumentException("Unknown sync job type: ${job.jobType}"))
            }
        }
    }
