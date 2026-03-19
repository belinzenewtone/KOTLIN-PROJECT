package com.personal.lifeOS.core.sync

import com.personal.lifeOS.core.database.entity.SyncJobEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface SyncStatusTracker {
    fun observeQueueState(): Flow<List<SyncJobEntity>>
}

@Singleton
class DefaultSyncStatusTracker
    @Inject
    constructor(
        private val syncQueueStore: SyncQueueStore,
    ) : SyncStatusTracker {
        override fun observeQueueState(): Flow<List<SyncJobEntity>> = syncQueueStore.observeJobs()
    }
