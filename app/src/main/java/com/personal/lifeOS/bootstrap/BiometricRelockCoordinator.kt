package com.personal.lifeOS.bootstrap

import com.personal.lifeOS.core.preferences.AppSettingsStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricRelockCoordinator
    @Inject
    constructor(
        private val appSettingsStore: AppSettingsStore,
    ) {
        suspend fun isRelockRequired(): Boolean = appSettingsStore.isBiometricEnabled()
    }
