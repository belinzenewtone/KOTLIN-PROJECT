package com.personal.lifeOS.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.core.notifications.ReminderSettingsCoordinator
import com.personal.lifeOS.core.preferences.AppSettingsStore
import com.personal.lifeOS.core.preferences.ThemePreferences
import com.personal.lifeOS.core.security.AuthSessionStore
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
                        infoMessage = if (enabled) "Notifications enabled" else "Notifications disabled and reminders cleared",
                    )
                }
            }
        }

        fun clearInfoMessage() {
            _uiState.update { it.copy(infoMessage = null) }
        }
    }
