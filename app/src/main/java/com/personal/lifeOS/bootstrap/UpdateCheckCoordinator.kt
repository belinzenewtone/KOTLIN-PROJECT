package com.personal.lifeOS.bootstrap

import com.personal.lifeOS.BuildConfig
import com.personal.lifeOS.core.datastore.FeatureFlag
import com.personal.lifeOS.core.datastore.FeatureFlagStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateCheckCoordinator
    @Inject
    constructor(
        private val featureFlagStore: FeatureFlagStore,
    ) {
        suspend fun shouldCheckForUpdates(): Boolean {
            return BuildConfig.OTA_MANIFEST_URL.isNotBlank() &&
                featureFlagStore.isEnabled(FeatureFlag.OTA_UPDATES)
        }
    }
