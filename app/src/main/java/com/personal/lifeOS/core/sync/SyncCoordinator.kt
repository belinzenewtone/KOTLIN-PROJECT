package com.personal.lifeOS.core.sync

import com.personal.lifeOS.core.database.entity.SyncJobEntity
import com.personal.lifeOS.core.sync.model.SyncTrigger
import kotlinx.coroutines.flow.Flow

interface SyncCoordinator {
    suspend fun enqueueDefault(trigger: SyncTrigger)

    suspend fun runPending(limit: Int = 50)

    fun observeQueue(): Flow<List<SyncJobEntity>>
}
