package com.personal.lifeOS.features.auth.presentation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.core.database.UserDataOwnershipService
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.features.auth.data.AuthResult
import com.personal.lifeOS.features.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false,
    val isSignUpMode: Boolean = false,
    // Sign in fields
    val email: String = "",
    val password: String = "",
    // Sign up fields
    val signUpUsername: String = "",
    val signUpEmail: String = "",
    val signUpPassword: String = "",
    val signUpConfirmPassword: String = "",
    // State
    val error: String? = null,
    val successMessage: String? = null,
    val showPassword: Boolean = false,
    // User info
    val userId: String = "",
    val username: String = "",
    val userEmail: String = "",
    val emailVerified: Boolean = false,
    val accountCreatedAt: String = "",
)

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val dataStore: DataStore<Preferences>,
        private val authSessionStore: AuthSessionStore,
        private val userDataOwnershipService: UserDataOwnershipService,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AuthUiState())
        val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

        companion object {
            val KEY_ACCESS_TOKEN = stringPreferencesKey("auth_access_token")
            val KEY_USER_ID = stringPreferencesKey("auth_user_id")
            val KEY_USER_EMAIL = stringPreferencesKey("auth_user_email")
            val KEY_USERNAME = stringPreferencesKey("auth_username")
            val KEY_EMAIL_VERIFIED = booleanPreferencesKey("auth_email_verified")
            val KEY_CREATED_AT = stringPreferencesKey("auth_created_at")
            val KEY_LOGGED_IN = booleanPreferencesKey("auth_logged_in")
        }

        init {
            checkExistingSession()
        }

        // Field updates
        fun updateEmail(v: String) {
            _uiState.update { it.copy(email = v) }
        }

        fun updatePassword(v: String) {
            _uiState.update { it.copy(password = v) }
        }

        fun updateSignUpUsername(v: String) {
            _uiState.update { it.copy(signUpUsername = v) }
        }

        fun updateSignUpEmail(v: String) {
            _uiState.update { it.copy(signUpEmail = v) }
        }

        fun updateSignUpPassword(v: String) {
            _uiState.update { it.copy(signUpPassword = v) }
        }

        fun updateSignUpConfirmPassword(v: String) {
            _uiState.update { it.copy(signUpConfirmPassword = v) }
        }

        fun togglePasswordVisibility() {
            _uiState.update { it.copy(showPassword = !it.showPassword) }
        }

        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }

        fun clearSuccess() {
            _uiState.update { it.copy(successMessage = null) }
        }

        fun switchToSignUp() {
            _uiState.update { it.copy(isSignUpMode = true, error = null) }
        }

        fun switchToSignIn() {
            _uiState.update { it.copy(isSignUpMode = false, error = null) }
        }

        fun signIn() {
            val state = _uiState.value
            if (state.email.isBlank() || state.password.isBlank()) {
                _uiState.update { it.copy(error = "Please fill in all fields") }
                return
            }

            _uiState.update { it.copy(isLoading = true, error = null) }

            viewModelScope.launch {
                when (val result = authRepository.signIn(state.email.trim(), state.password)) {
                    is AuthResult.Success -> {
                        saveSession(result)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                userId = result.userId,
                                userEmail = result.email,
                                username = result.username,
                                emailVerified = result.emailConfirmed,
                                accountCreatedAt = result.createdAt,
                            )
                        }
                    }
                    is AuthResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            }
        }

        fun signUp() {
            val state = _uiState.value

            if (state.signUpUsername.isBlank() || state.signUpEmail.isBlank() ||
                state.signUpPassword.isBlank() || state.signUpConfirmPassword.isBlank()
            ) {
                _uiState.update { it.copy(error = "Please fill in all fields") }
                return
            }
            if (state.signUpPassword.length < 6) {
                _uiState.update { it.copy(error = "Password must be at least 6 characters") }
                return
            }
            if (state.signUpPassword != state.signUpConfirmPassword) {
                _uiState.update { it.copy(error = "Passwords don't match") }
                return
            }
            if (!state.signUpEmail.contains("@")) {
                _uiState.update { it.copy(error = "Please enter a valid email") }
                return
            }

            _uiState.update { it.copy(isLoading = true, error = null) }

            viewModelScope.launch {
                when (
                    val result =
                        authRepository.signUp(
                            state.signUpEmail.trim(),
                            state.signUpPassword,
                            state.signUpUsername.trim(),
                        )
                ) {
                    is AuthResult.Success -> {
                        saveSession(result)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                userId = result.userId,
                                userEmail = result.email,
                                username = result.username,
                                emailVerified = result.emailConfirmed,
                                accountCreatedAt = result.createdAt,
                                successMessage = "Account created! Check your email for a verification link.",
                            )
                        }
                    }
                    is AuthResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            }
        }

        fun resendVerification() {
            viewModelScope.launch {
                val email = _uiState.value.userEmail
                if (email.isNotBlank()) {
                    val sent = authRepository.resendVerification(email)
                    _uiState.update {
                        it.copy(
                            successMessage = if (sent) "Verification email sent to $email" else "Failed to send email",
                        )
                    }
                }
            }
        }

        fun signOut() {
            viewModelScope.launch {
                authSessionStore.clearSession()
                dataStore.edit { prefs ->
                    prefs.remove(KEY_ACCESS_TOKEN)
                    prefs.remove(KEY_USER_ID)
                    prefs.remove(KEY_USER_EMAIL)
                    prefs.remove(KEY_USERNAME)
                    prefs.remove(KEY_EMAIL_VERIFIED)
                    prefs.remove(KEY_CREATED_AT)
                    prefs.remove(KEY_LOGGED_IN)
                }
                _uiState.update {
                    AuthUiState(isLoading = false, isLoggedIn = false)
                }
            }
        }

        fun refreshUser() {
            viewModelScope.launch {
                val token = authSessionStore.getAccessToken()
                if (token.isBlank()) return@launch

                when (val result = authRepository.getUser(token)) {
                    is AuthResult.Success -> {
                        authSessionStore.saveSession(token, result.userId)
                        userDataOwnershipService.claimUnownedData(result.userId)
                        dataStore.edit {
                            it[KEY_USER_ID] = result.userId
                            it[KEY_EMAIL_VERIFIED] = result.emailConfirmed
                        }
                        _uiState.update {
                            it.copy(emailVerified = result.emailConfirmed)
                        }
                    }
                    is AuthResult.Error -> { /* silently fail */ }
                }
            }
        }

        private fun checkExistingSession() {
            viewModelScope.launch {
                val prefs = dataStore.data.first()
                val loggedIn = prefs[KEY_LOGGED_IN] ?: false
                val secureToken = authSessionStore.getAccessToken()
                val legacyToken = prefs[KEY_ACCESS_TOKEN] ?: ""
                val token = if (secureToken.isNotBlank()) secureToken else legacyToken

                if (loggedIn && token.isNotBlank()) {
                    if (secureToken.isBlank()) {
                        authSessionStore.saveSession(token, prefs[KEY_USER_ID].orEmpty())
                        dataStore.edit { it.remove(KEY_ACCESS_TOKEN) }
                    }

                    // Verify token still valid
                    when (val result = authRepository.getUser(token)) {
                        is AuthResult.Success -> {
                            authSessionStore.saveSession(token, result.userId)
                            userDataOwnershipService.claimUnownedData(result.userId)
                            dataStore.edit {
                                it[KEY_USER_ID] = result.userId
                                it[KEY_USER_EMAIL] = result.email
                                it[KEY_USERNAME] = result.username
                                it[KEY_EMAIL_VERIFIED] = result.emailConfirmed
                                it[KEY_CREATED_AT] = result.createdAt
                                it[KEY_LOGGED_IN] = true
                                it.remove(KEY_ACCESS_TOKEN)
                            }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = true,
                                    userId = result.userId,
                                    userEmail = result.email,
                                    username = result.username,
                                    emailVerified = result.emailConfirmed,
                                    accountCreatedAt = result.createdAt,
                                )
                            }
                        }
                        is AuthResult.Error -> {
                            // Token expired, show login
                            authSessionStore.clearSession()
                            dataStore.edit { it[KEY_LOGGED_IN] = false }
                            _uiState.update { it.copy(isLoading = false, isLoggedIn = false) }
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = false) }
                }
            }
        }

        private suspend fun saveSession(result: AuthResult.Success) {
            authSessionStore.saveSession(result.accessToken, result.userId)
            userDataOwnershipService.claimUnownedData(result.userId)
            dataStore.edit { prefs ->
                prefs[KEY_USER_ID] = result.userId
                prefs[KEY_USER_EMAIL] = result.email
                prefs[KEY_USERNAME] = result.username
                prefs[KEY_EMAIL_VERIFIED] = result.emailConfirmed
                prefs[KEY_CREATED_AT] = result.createdAt
                prefs[KEY_LOGGED_IN] = true
                prefs.remove(KEY_ACCESS_TOKEN)
            }
        }
    }
