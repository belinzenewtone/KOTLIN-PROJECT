package com.personal.lifeOS.features.settings.presentation

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
import com.personal.lifeOS.ui.theme.AppThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val syncHealth: SyncHealthSummary = SyncHealthSummary(),
    val importHealth: ImportHealthSummary = ImportHealthSummary(),
    val latestUpdateInfo: AppUpdateInfo? = null,
    val featureFlags: Map<FeatureFlag, Boolean> = emptyMap(),
    val infoMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val themePreferences: ThemePreferences,
        private val appSettingsStore: AppSettingsStore,
        private val authSessionStore: AuthSessionStore,
        private val reminderSettingsCoordinator: ReminderSettingsCoordinator,
        private val healthDiagnosticsRepository: HealthDiagnosticsRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState())
        val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

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

        fun setThemeMode(mode: AppThemeMode) {
            viewModelScope.launch {
                themePreferences.setThemeMode(mode)
                _uiState.update { it.copy(infoMessage = "Theme mode updated") }
            }
        }

        fun setNotificationsEnabled(enabled: Boolean) {
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

        fun clearInfoMessage() {
            _uiState.update { it.copy(infoMessage = null) }
        }

        fun refreshFeatureFlags() {
            viewModelScope.launch {
                val flags = healthDiagnosticsRepository.featureFlagSnapshot()
                _uiState.update { it.copy(featureFlags = flags) }
            }
        }
    }
