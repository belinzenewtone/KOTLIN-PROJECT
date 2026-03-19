package com.personal.lifeOS.features.auth.presentation

sealed interface AuthUiEvent {
    data class UpdateEmail(val value: String) : AuthUiEvent

    data class UpdatePassword(val value: String) : AuthUiEvent

    data class UpdateSignUpUsername(val value: String) : AuthUiEvent

    data class UpdateSignUpEmail(val value: String) : AuthUiEvent

    data class UpdateSignUpPassword(val value: String) : AuthUiEvent

    data class UpdateSignUpConfirmPassword(val value: String) : AuthUiEvent

    data object TogglePasswordVisibility : AuthUiEvent

    data object ClearError : AuthUiEvent

    data object ClearSuccess : AuthUiEvent

    data object SwitchToSignUp : AuthUiEvent

    data object SwitchToSignIn : AuthUiEvent

    data object SignIn : AuthUiEvent

    data object SignUp : AuthUiEvent

    data object SendPasswordReset : AuthUiEvent

    data object ResendVerification : AuthUiEvent

    data object SignOut : AuthUiEvent

    data object RefreshUser : AuthUiEvent
}
