package com.personal.lifeOS.bootstrap

import com.personal.lifeOS.core.datastore.RefreshFeatureFlagsUseCase
import com.personal.lifeOS.core.preferences.AppSettingsStore
import com.personal.lifeOS.core.update.UpdateCheckUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppBootstrapCoordinator
    @Suppress("LongParameterList")
    @Inject
    constructor(
        private val sessionHydrationUseCase: SessionHydrationUseCase,
        private val permissionPromptCoordinator: PermissionPromptCoordinator,
        private val updateCheckCoordinator: UpdateCheckCoordinator,
        private val updateCheckUseCase: UpdateCheckUseCase,
        private val refreshFeatureFlagsUseCase: RefreshFeatureFlagsUseCase,
        private val initialSyncCoordinator: InitialSyncCoordinator,
        private val biometricRelockCoordinator: BiometricRelockCoordinator,
        private val appSettingsStore: AppSettingsStore,
    ) {
        suspend fun bootstrap(): BootstrapResult {
            val hasSession = sessionHydrationUseCase()
            permissionPromptCoordinator.prepareStartupPermissionRouting()
            runCatching { refreshFeatureFlagsUseCase() }
            if (hasSession) {
                initialSyncCoordinator.triggerInitialSync()
            }
            val shouldCheckForUpdates = updateCheckCoordinator.shouldCheckForUpdates()
            if (shouldCheckForUpdates) {
                runCatching { updateCheckUseCase() }
            }
            val onboardingCompleted = if (hasSession) appSettingsStore.isOnboardingCompleted() else false
            val startDestination =
                when {
                    !hasSession -> StartDestination.AUTH
                    onboardingCompleted -> StartDestination.HOME
                    else -> StartDestination.ONBOARDING
                }
            return BootstrapResult(
                startDestination = startDestination,
                requiresBiometricRelock = biometricRelockCoordinator.isRelockRequired(),
                shouldCheckForUpdates = shouldCheckForUpdates,
            )
        }
    }
