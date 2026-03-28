package com.personal.lifeOS.features.profile.domain.usecase

import com.personal.lifeOS.core.sync.SyncCoordinator
import com.personal.lifeOS.core.sync.model.SyncTrigger
import javax.inject.Inject

class PushCloudSyncUseCase
    @Inject
    constructor(
        private val syncCoordinator: SyncCoordinator,
    ) {
        suspend operator fun invoke(): String {
            syncCoordinator.enqueueDefault(SyncTrigger.USER_PULL_TO_REFRESH)
            syncCoordinator.runPending()
            return "Queued sync started. Recent changes are moving through the sync pipeline."
        }
    }
