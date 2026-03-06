package com.personal.lifeOS.features.profile.presentation

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.features.profile.domain.model.UserProfile
import com.personal.lifeOS.core.utils.CloudSyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val isEditing: Boolean = false,
    val editName: String = "",
    val editEmail: String = "",
    val editPhone: String = "",
    val showPasswordDialog: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordError: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val cloudSyncService: CloudSyncService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    companion object {
        val KEY_NAME = stringPreferencesKey("user_name")
        val KEY_EMAIL = stringPreferencesKey("user_email")
        val KEY_PHONE = stringPreferencesKey("user_phone")
        val KEY_PASSWORD = stringPreferencesKey("user_password")
        val KEY_BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val KEY_PROFILE_PIC = stringPreferencesKey("profile_pic_path")
        val KEY_MEMBER_SINCE = longPreferencesKey("member_since")
    }

    init {
        loadProfile()
    }

    fun startEditing() {
        val profile = _uiState.value.profile
        _uiState.update {
            it.copy(
                isEditing = true,
                editName = profile.name,
                editEmail = profile.email,
                editPhone = profile.phone
            )
        }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false) }
    }

    fun updateEditName(name: String) { _uiState.update { it.copy(editName = name) } }
    fun updateEditEmail(email: String) { _uiState.update { it.copy(editEmail = email) } }
    fun updateEditPhone(phone: String) { _uiState.update { it.copy(editPhone = phone) } }

    fun saveProfile() {
        viewModelScope.launch {
            val state = _uiState.value

            // Set member since on first save
            val prefs = dataStore.data.first()
            val existingMemberSince = prefs[KEY_MEMBER_SINCE] ?: 0L

            dataStore.edit { p ->
                p[KEY_NAME] = state.editName
                p[KEY_EMAIL] = state.editEmail
                p[KEY_PHONE] = state.editPhone
                if (existingMemberSince == 0L) {
                    p[KEY_MEMBER_SINCE] = System.currentTimeMillis()
                }
            }

            val initials = state.editName.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .joinToString("")

            val memberSince = if (existingMemberSince == 0L) System.currentTimeMillis() else existingMemberSince

            _uiState.update {
                it.copy(
                    profile = it.profile.copy(
                        name = state.editName,
                        email = state.editEmail,
                        phone = state.editPhone,
                        avatarInitials = initials.ifEmpty { "?" },
                        memberSince = memberSince
                    ),
                    isEditing = false,
                    successMessage = "Profile saved"
                )
            }
        }
    }

    /**
     * Copy the selected image to app-private storage and save path.
     */
    fun updateProfilePic(uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val file = File(context.filesDir, "profile_pic.jpg")
                FileOutputStream(file).use { output ->
                    inputStream.copyTo(output)
                }
                inputStream.close()

                val path = file.absolutePath
                dataStore.edit { it[KEY_PROFILE_PIC] = path }

                _uiState.update {
                    it.copy(
                        profile = it.profile.copy(profilePicUri = path),
                        successMessage = "Profile photo updated"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(successMessage = "Failed to update photo") }
            }
        }
    }

    fun showPasswordDialog() {
        _uiState.update {
            it.copy(
                showPasswordDialog = true,
                currentPassword = "",
                newPassword = "",
                confirmPassword = "",
                passwordError = null
            )
        }
    }

    fun hidePasswordDialog() {
        _uiState.update { it.copy(showPasswordDialog = false) }
    }

    fun updateCurrentPassword(pw: String) { _uiState.update { it.copy(currentPassword = pw) } }
    fun updateNewPassword(pw: String) { _uiState.update { it.copy(newPassword = pw) } }
    fun updateConfirmPassword(pw: String) { _uiState.update { it.copy(confirmPassword = pw) } }

    fun changePassword() {
        val state = _uiState.value
        viewModelScope.launch {
            val storedPassword = dataStore.data.first()[KEY_PASSWORD] ?: ""

            if (storedPassword.isNotEmpty() && state.currentPassword != storedPassword) {
                _uiState.update { it.copy(passwordError = "Current password is incorrect") }
                return@launch
            }
            if (state.newPassword.length < 6) {
                _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
                return@launch
            }
            if (state.newPassword != state.confirmPassword) {
                _uiState.update { it.copy(passwordError = "Passwords don't match") }
                return@launch
            }

            dataStore.edit { it[KEY_PASSWORD] = state.newPassword }
            _uiState.update {
                it.copy(showPasswordDialog = false, successMessage = "Password updated")
            }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_BIOMETRIC] = enabled }
            _uiState.update { it.copy(profile = it.profile.copy(isBiometricEnabled = enabled)) }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
            _uiState.update { it.copy(profile = it.profile.copy(notificationsEnabled = enabled)) }
        }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun syncToCloud() {
        viewModelScope.launch {
            _uiState.update { it.copy(successMessage = "Syncing to cloud...") }
            val result = cloudSyncService.pushToCloud()
            _uiState.update { it.copy(successMessage = result.message) }
        }
    }

    fun syncFromCloud() {
        viewModelScope.launch {
            _uiState.update { it.copy(successMessage = "Restoring from cloud...") }
            val result = cloudSyncService.pullFromCloud()
            _uiState.update { it.copy(successMessage = result.message) }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            val name = prefs[KEY_NAME] ?: ""
            val email = prefs[KEY_EMAIL] ?: ""
            val phone = prefs[KEY_PHONE] ?: ""
            val biometric = prefs[KEY_BIOMETRIC] ?: false
            val notifications = prefs[KEY_NOTIFICATIONS] ?: true
            val picPath = prefs[KEY_PROFILE_PIC] ?: ""
            val memberSince = prefs[KEY_MEMBER_SINCE] ?: 0L

            val initials = name.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .joinToString("")

            _uiState.update {
                it.copy(
                    profile = UserProfile(
                        name = name,
                        email = email,
                        phone = phone,
                        avatarInitials = initials.ifEmpty { "?" },
                        profilePicUri = picPath,
                        memberSince = memberSince,
                        isBiometricEnabled = biometric,
                        notificationsEnabled = notifications
                    )
                )
            }
        }
    }
}
