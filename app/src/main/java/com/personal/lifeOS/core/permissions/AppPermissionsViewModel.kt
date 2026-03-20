package com.personal.lifeOS.core.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.core.preferences.AppSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lightweight ViewModel that bridges [AppSettingsStore] permission-ask flags
 * to the [AppPermissionsOrchestrator] composable.
 *
 * This ViewModel is scoped to the NavHost lifecycle so the rationale cards
 * survive configuration changes and aren't re-shown on rotation.
 */
@HiltViewModel
class AppPermissionsViewModel @Inject constructor(
    private val appSettingsStore: AppSettingsStore,
) : ViewModel() {

    // true  → we've already shown the rationale once; never show again
    // false → haven't asked yet; safe to show the rationale card
    private val _notificationPermissionAsked = MutableStateFlow(true) // start true to avoid flicker
    val notificationPermissionAsked: StateFlow<Boolean> = _notificationPermissionAsked.asStateFlow()

    private val _smsPermissionAsked = MutableStateFlow(true)
    val smsPermissionAsked: StateFlow<Boolean> = _smsPermissionAsked.asStateFlow()

    init {
        viewModelScope.launch {
            _notificationPermissionAsked.value = appSettingsStore.wasNotificationPermissionAsked()
            _smsPermissionAsked.value = appSettingsStore.wasSmsPermissionAsked()
        }
    }

    fun markNotificationPermissionAsked() {
        _notificationPermissionAsked.value = true
        viewModelScope.launch { appSettingsStore.markNotificationPermissionAsked() }
    }

    fun markSmsPermissionAsked() {
        _smsPermissionAsked.value = true
        viewModelScope.launch { appSettingsStore.markSmsPermissionAsked() }
    }
}
