package com.personal.lifeOS.bootstrap

import com.personal.lifeOS.core.sync.SyncCoordinator
import com.personal.lifeOS.core.sync.model.SyncTrigger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InitialSyncCoordinator
    @Inject
    constructor(
        private val syncCoordinator: SyncCoordinator,
    ) {
        suspend fun triggerInitialSync() {
            syncCoordinator.enqueueDefault(SyncTrigger.APP_START)
            syncCoordinator.runPending(limit = 20)
        }
    }
