package com.personal.lifeOS.features.profile.domain.usecase

import com.personal.lifeOS.core.sync.SyncCoordinator
import com.personal.lifeOS.core.sync.model.SyncTrigger
import javax.inject.Inject

class PullCloudSyncUseCase
    @Inject
    constructor(
        private val syncCoordinator: SyncCoordinator,
    ) {
        suspend operator fun invoke(): String {
            syncCoordinator.enqueueDefault(SyncTrigger.USER_PULL_TO_REFRESH)
            syncCoordinator.runPending()
            return "Refresh sync started. The queue will push local changes and pull the latest cloud state."
        }
    }
