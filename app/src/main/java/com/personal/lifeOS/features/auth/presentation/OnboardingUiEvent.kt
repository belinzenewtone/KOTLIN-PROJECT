package com.personal.lifeOS.features.auth.presentation

sealed interface OnboardingUiEvent {
    data object Continue : OnboardingUiEvent

    data object GoBack : OnboardingUiEvent

    data class UpdateFullName(val value: String) : OnboardingUiEvent

    data class SelectGoal(val goal: OnboardingGoal) : OnboardingUiEvent

    data object Complete : OnboardingUiEvent

    data object DismissError : OnboardingUiEvent
}

