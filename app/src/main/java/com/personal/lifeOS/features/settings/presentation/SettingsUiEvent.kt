package com.personal.lifeOS.features.settings.presentation

import com.personal.lifeOS.core.update.OtaInstallResult
import com.personal.lifeOS.ui.theme.AppThemeMode

sealed interface SettingsUiEvent {
    data class SetThemeMode(val mode: AppThemeMode) : SettingsUiEvent

    data class ToggleNotifications(val enabled: Boolean) : SettingsUiEvent

    data object RefreshFeatureFlags : SettingsUiEvent

    data object CheckForOtaUpdate : SettingsUiEvent

    data object DownloadAndInstallOtaUpdate : SettingsUiEvent

    data object InstallDownloadedOtaUpdate : SettingsUiEvent

    data object ClearInfoMessage : SettingsUiEvent

    data class InstallerResult(val result: OtaInstallResult) : SettingsUiEvent
}
