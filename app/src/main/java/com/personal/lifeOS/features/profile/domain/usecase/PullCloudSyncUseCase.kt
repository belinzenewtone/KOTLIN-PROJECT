package com.personal.lifeOS.features.profile.domain.usecase

import com.personal.lifeOS.core.utils.CloudSyncService
import javax.inject.Inject

class PullCloudSyncUseCase
    @Inject
    constructor(
        private val cloudSyncService: CloudSyncService,
    ) {
        suspend operator fun invoke(): String {
            return cloudSyncService.pullFromCloud().message
        }
    }
