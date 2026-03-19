package com.personal.lifeOS.features.auth.presentation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.core.preferences.AppSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingGoal(
    val key: String,
    val title: String,
    val description: String,
) {
    PRODUCTIVITY(
        key = "productivity",
        title = "Optimize Productivity",
        description = "Sharper focus, smarter routines, better execution.",
    ),
    FINANCE(
        key = "finance",
        title = "Strengthen Finance",
        description = "Track spending and budgets with clear control.",
    ),
    BALANCED(
        key = "balanced",
        title = "Balance Everything",
        description = "Plan work, money, and time in one calm system.",
    ),
    ;

    companion object {
        fun fromKey(value: String): OnboardingGoal {
            return entries.firstOrNull { it.key == value } ?: PRODUCTIVITY
        }
    }
}

data class OnboardingUiState(
    val currentStep: Int = 1,
    val fullName: String = "",
    val primaryGoal: OnboardingGoal = OnboardingGoal.PRODUCTIVITY,
    val isSaving: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val appSettingsStore: AppSettingsStore,
        private val dataStore: DataStore<Preferences>,
    ) : ViewModel() {
        private val profileNameKey = stringPreferencesKey("user_name")

        private val _uiState = MutableStateFlow(OnboardingUiState())
        val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

        init {
            hydrateState()
        }

        fun onEvent(event: OnboardingUiEvent) {
            when (event) {
                OnboardingUiEvent.Continue -> continueFromStep()
                OnboardingUiEvent.GoBack -> goBack()
                is OnboardingUiEvent.UpdateFullName -> {
                    _uiState.update { it.copy(fullName = event.value, errorMessage = null) }
                }
                is OnboardingUiEvent.SelectGoal -> {
                    _uiState.update { it.copy(primaryGoal = event.goal, errorMessage = null) }
                    viewModelScope.launch {
                        appSettingsStore.setOnboardingPrimaryGoal(event.goal.key)
                    }
                }
                OnboardingUiEvent.Complete -> complete()
                OnboardingUiEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
            }
        }

        private fun hydrateState() {
            viewModelScope.launch {
                val profileName = dataStore.data.first()[profileNameKey].orEmpty()
                val onboardingCompleted = appSettingsStore.isOnboardingCompleted()
                val onboardingStep = appSettingsStore.getOnboardingStep().coerceIn(1, 4)
                val goal = OnboardingGoal.fromKey(appSettingsStore.getOnboardingPrimaryGoal())
                _uiState.update {
                    it.copy(
                        currentStep = onboardingStep,
                        fullName = profileName,
                        primaryGoal = goal,
                        isCompleted = onboardingCompleted,
                    )
                }
            }
        }

        private fun continueFromStep() {
            val state = _uiState.value
            if (state.currentStep == 3 && state.fullName.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Please provide your full name to continue.") }
                return
            }
            if (state.currentStep >= 4) {
                complete()
                return
            }

            val nextStep = state.currentStep + 1
            _uiState.update { it.copy(currentStep = nextStep, errorMessage = null) }
            viewModelScope.launch {
                appSettingsStore.setOnboardingStep(nextStep)
            }
        }

        private fun goBack() {
            val state = _uiState.value
            if (state.currentStep <= 1) return
            val nextStep = state.currentStep - 1
            _uiState.update { it.copy(currentStep = nextStep, errorMessage = null) }
            viewModelScope.launch {
                appSettingsStore.setOnboardingStep(nextStep)
            }
        }

        private fun complete() {
            val state = _uiState.value
            if (state.fullName.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Please provide your full name before finishing.") }
                return
            }
            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, errorMessage = null) }
                appSettingsStore.completeOnboarding(
                    fullName = state.fullName,
                    primaryGoal = state.primaryGoal.key,
                )
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isCompleted = true,
                        currentStep = 4,
                    )
                }
            }
        }
    }

