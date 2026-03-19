package com.personal.lifeOS.features.settings.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.core.datastore.FeatureFlag
import com.personal.lifeOS.core.notifications.ReminderSettingsCoordinator
import com.personal.lifeOS.core.preferences.AppSettingsStore
import com.personal.lifeOS.core.preferences.ThemePreferences
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.core.telemetry.HealthDiagnosticsRepository
import com.personal.lifeOS.core.telemetry.ImportHealthSummary
import com.personal.lifeOS.core.telemetry.SyncHealthSummary
import com.personal.lifeOS.core.update.AppUpdateInfo
import com.personal.lifeOS.core.update.OtaCheckResult
import com.personal.lifeOS.core.update.OtaDownloadResult
import com.personal.lifeOS.core.update.OtaInstallResult
import com.personal.lifeOS.core.update.OtaUpdateManifest
import com.personal.lifeOS.features.settings.domain.usecase.CheckForSettingsOtaUpdateUseCase
import com.personal.lifeOS.features.settings.domain.usecase.DownloadSettingsOtaUpdateUseCase
import com.personal.lifeOS.ui.theme.AppThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OtaAvailableUpdateUiModel(
    val versionCode: Long,
    val versionName: String?,
    val changelog: String?,
)

data class SettingsOtaUiState(
    val isBusy: Boolean = false,
    val downloadProgress: Int? = null,
    val statusMessage: String = "Check and install APK updates from your OTA server.",
    val availableUpdate: OtaAvailableUpdateUiModel? = null,
    val hasDownloadedApk: Boolean = false,
)

sealed interface SettingsUiEffect {
    data class LaunchOtaInstaller(val apkUri: Uri) : SettingsUiEffect
}

data class SettingsUiState(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val syncHealth: SyncHealthSummary = SyncHealthSummary(),
    val importHealth: ImportHealthSummary = ImportHealthSummary(),
    val latestUpdateInfo: AppUpdateInfo? = null,
    val featureFlags: Map<FeatureFlag, Boolean> = emptyMap(),
    val otaUpdate: SettingsOtaUiState = SettingsOtaUiState(),
    val infoMessage: String? = null,
)

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val themePreferences: ThemePreferences,
        private val appSettingsStore: AppSettingsStore,
        private val authSessionStore: AuthSessionStore,
        private val reminderSettingsCoordinator: ReminderSettingsCoordinator,
        private val healthDiagnosticsRepository: HealthDiagnosticsRepository,
        private val checkForSettingsOtaUpdateUseCase: CheckForSettingsOtaUpdateUseCase,
        private val downloadSettingsOtaUpdateUseCase: DownloadSettingsOtaUpdateUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState())
        val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
        private val _uiEffects = MutableSharedFlow<SettingsUiEffect>(extraBufferCapacity = 1)
        val uiEffects: SharedFlow<SettingsUiEffect> = _uiEffects.asSharedFlow()

        private var availableManifest: OtaUpdateManifest? = null
        private var downloadedApkUri: Uri? = null

        init {
            themePreferences.themeModeFlow()
                .onEach { mode -> _uiState.update { it.copy(themeMode = mode) } }
                .launchIn(viewModelScope)

            appSettingsStore.notificationsEnabledFlow()
                .onEach { enabled -> _uiState.update { it.copy(notificationsEnabled = enabled) } }
                .launchIn(viewModelScope)

            healthDiagnosticsRepository.observeSyncHealth()
                .onEach { summary -> _uiState.update { it.copy(syncHealth = summary) } }
                .launchIn(viewModelScope)

            healthDiagnosticsRepository.observeImportHealth()
                .onEach { summary -> _uiState.update { it.copy(importHealth = summary) } }
                .launchIn(viewModelScope)

            healthDiagnosticsRepository.observeLatestUpdateInfo()
                .onEach { info -> _uiState.update { it.copy(latestUpdateInfo = info) } }
                .launchIn(viewModelScope)

            refreshFeatureFlags()
        }

        fun onEvent(event: SettingsUiEvent) {
            when (event) {
                is SettingsUiEvent.SetThemeMode -> setThemeMode(event.mode)
                is SettingsUiEvent.ToggleNotifications -> setNotificationsEnabled(event.enabled)
                SettingsUiEvent.RefreshFeatureFlags -> refreshFeatureFlags()
                SettingsUiEvent.CheckForOtaUpdate -> checkForOtaUpdate()
                SettingsUiEvent.DownloadAndInstallOtaUpdate -> downloadAndInstallOtaUpdate()
                SettingsUiEvent.InstallDownloadedOtaUpdate -> installDownloadedOtaUpdate()
                SettingsUiEvent.ClearInfoMessage -> clearInfoMessage()
                is SettingsUiEvent.InstallerResult -> onInstallerResult(event.result)
            }
        }

        private fun setThemeMode(mode: AppThemeMode) {
            viewModelScope.launch {
                themePreferences.setThemeMode(mode)
                _uiState.update { it.copy(infoMessage = "Theme mode updated") }
            }
        }

        private fun setNotificationsEnabled(enabled: Boolean) {
            viewModelScope.launch {
                appSettingsStore.setNotificationsEnabled(enabled)
                if (!enabled) {
                    reminderSettingsCoordinator.cancelAllScheduledRemindersForUser(authSessionStore.getUserId())
                }
                _uiState.update {
                    it.copy(
                        infoMessage =
                            if (enabled) {
                                "Notifications enabled"
                            } else {
                                "Notifications disabled and reminders cleared"
                            },
                    )
                }
            }
        }

        private fun clearInfoMessage() {
            _uiState.update { it.copy(infoMessage = null) }
        }

        private fun refreshFeatureFlags() {
            viewModelScope.launch {
                val flags = healthDiagnosticsRepository.featureFlagSnapshot()
                _uiState.update { it.copy(featureFlags = flags) }
            }
        }

        private fun checkForOtaUpdate() {
            viewModelScope.launch {
                availableManifest = null
                downloadedApkUri = null
                updateOtaState {
                    it.copy(
                        isBusy = true,
                        downloadProgress = null,
                        availableUpdate = null,
                        hasDownloadedApk = false,
                    )
                }
                val nextStatus =
                    when (val result = checkForSettingsOtaUpdateUseCase()) {
                        is OtaCheckResult.UpdateAvailable -> {
                            availableManifest = result.manifest
                            updateOtaState {
                                it.copy(
                                    availableUpdate =
                                        OtaAvailableUpdateUiModel(
                                            versionCode = result.manifest.versionCode,
                                            versionName = result.manifest.versionName,
                                            changelog = result.manifest.changelog,
                                        ),
                                )
                            }
                            "Update found. Ready to download and install."
                        }

                        OtaCheckResult.UpToDate -> "You're on the latest version."
                        OtaCheckResult.NotConfigured -> "OTA_MANIFEST_URL is not configured in local.properties."
                        is OtaCheckResult.Error -> "Update check failed: ${result.message}"
                    }
                updateOtaState {
                    it.copy(
                        isBusy = false,
                        statusMessage = nextStatus,
                        downloadProgress = null,
                    )
                }
            }
        }

        private fun downloadAndInstallOtaUpdate() {
            val manifest = availableManifest
            if (manifest == null) {
                updateOtaState { it.copy(statusMessage = "No update available to download.") }
                return
            }

            viewModelScope.launch {
                updateOtaState {
                    it.copy(
                        isBusy = true,
                        statusMessage = "Downloading update APK...",
                        downloadProgress = null,
                    )
                }
                when (
                    val result =
                        downloadSettingsOtaUpdateUseCase(
                            manifest = manifest,
                            onProgress = { progress ->
                                updateOtaState {
                                    it.copy(
                                        isBusy = true,
                                        statusMessage = "Downloading update APK...",
                                        downloadProgress = progress,
                                    )
                                }
                            },
                        )
                ) {
                    is OtaDownloadResult.Success -> {
                        downloadedApkUri = result.apkUri
                        updateOtaState {
                            it.copy(
                                isBusy = false,
                                downloadProgress = 100,
                                hasDownloadedApk = true,
                                statusMessage = "Download complete. Opening installer...",
                            )
                        }
                        _uiEffects.emit(SettingsUiEffect.LaunchOtaInstaller(result.apkUri))
                    }

                    OtaDownloadResult.Cancelled -> {
                        updateOtaState {
                            it.copy(
                                isBusy = false,
                                statusMessage = "Download cancelled.",
                            )
                        }
                    }

                    is OtaDownloadResult.Error -> {
                        updateOtaState {
                            it.copy(
                                isBusy = false,
                                statusMessage = "Download failed: ${result.message}",
                            )
                        }
                    }
                }
            }
        }

        private fun installDownloadedOtaUpdate() {
            val apkUri = downloadedApkUri
            if (apkUri == null) {
                updateOtaState { it.copy(statusMessage = "No downloaded APK available to install.") }
                return
            }
            viewModelScope.launch {
                _uiEffects.emit(SettingsUiEffect.LaunchOtaInstaller(apkUri))
            }
        }

        private fun onInstallerResult(result: OtaInstallResult) {
            updateOtaState { it.copy(statusMessage = mapInstallStatus(result)) }
        }

        private fun updateOtaState(transform: (SettingsOtaUiState) -> SettingsOtaUiState) {
            _uiState.update { state ->
                state.copy(otaUpdate = transform(state.otaUpdate))
            }
        }

        private fun mapInstallStatus(result: OtaInstallResult): String {
            return when (result) {
                OtaInstallResult.Started -> "Installer opened. Confirm installation to complete update."
                OtaInstallResult.RequiresUnknownSourcesPermission ->
                    "Allow installs from this app, then tap install again."

                is OtaInstallResult.Error -> "Install launch failed: ${result.message}"
            }
        }
    }
