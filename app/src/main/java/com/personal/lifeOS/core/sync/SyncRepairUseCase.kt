package com.personal.lifeOS.core.sync

import com.personal.lifeOS.core.sync.model.SyncTrigger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepairUseCase
    @Inject
    constructor(
        private val syncCoordinator: SyncCoordinator,
    ) {
        suspend operator fun invoke() {
            syncCoordinator.enqueueDefault(SyncTrigger.USER_MANUAL_RETRY)
            syncCoordinator.runPending()
        }
    }
