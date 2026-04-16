package com.personal.lifeOS.features.recurring.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.features.recurring.domain.model.RecurringCadence
import com.personal.lifeOS.features.recurring.domain.model.RecurringRule
import com.personal.lifeOS.features.recurring.domain.model.RecurringType
import com.personal.lifeOS.features.recurring.domain.repository.RecurringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringUiState(
    val rules: List<RecurringRule> = emptyList(),
    val showDialog: Boolean = false,
    val titleInput: String = "",
    val amountInput: String = "",
    val typeInput: RecurringType = RecurringType.EXPENSE,
    val cadenceInput: RecurringCadence = RecurringCadence.MONTHLY,
    val error: String? = null,
)

@HiltViewModel
class RecurringViewModel
    @Inject
    constructor(
        private val repository: RecurringRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(RecurringUiState())
        val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

        init {
            repository.getRules()
                .onEach { rules -> _uiState.update { it.copy(rules = rules) } }
                .launchIn(viewModelScope)
        }

        fun showAddDialog() {
            _uiState.update {
                it.copy(
                    showDialog = true,
                    titleInput = "",
                    amountInput = "",
                    typeInput = RecurringType.EXPENSE,
                    cadenceInput = RecurringCadence.MONTHLY,
                    error = null,
                )
            }
        }

        fun hideDialog() {
            _uiState.update { it.copy(showDialog = false, error = null) }
        }

        fun setTitle(value: String) {
            _uiState.update { it.copy(titleInput = value) }
        }

        fun setAmount(value: String) {
            _uiState.update { it.copy(amountInput = value) }
        }

        fun setType(value: RecurringType) {
            _uiState.update { it.copy(typeInput = value) }
        }

        fun setCadence(value: RecurringCadence) {
            _uiState.update { it.copy(cadenceInput = value) }
        }

        fun saveRule() {
            val state = _uiState.value
            val title = state.titleInput.trim()
            val amount = state.amountInput.toDoubleOrNull()

            if (title.isBlank()) {
                _uiState.update { it.copy(error = "Title is required") }
                return
            }

            if (state.typeInput != RecurringType.TASK && (amount == null || amount <= 0.0)) {
                _uiState.update { it.copy(error = "Enter a valid amount") }
                return
            }

            viewModelScope.launch {
                repository.addRule(
                    RecurringRule(
                        title = title,
                        type = state.typeInput,
                        cadence = state.cadenceInput,
                        amount = if (state.typeInput == RecurringType.TASK) null else amount,
                        nextRunAt = System.currentTimeMillis() + cadenceToMillis(state.cadenceInput),
                    ),
                )
                hideDialog()
            }
        }

        fun deleteRule(id: Long) {
            viewModelScope.launch {
                repository.deleteRule(id)
            }
        }

        fun toggleEnabled(
            rule: RecurringRule,
            enabled: Boolean,
        ) {
            viewModelScope.launch {
                repository.setEnabled(rule.id, enabled)
            }
        }

        private fun cadenceToMillis(cadence: RecurringCadence): Long {
            return when (cadence) {
                RecurringCadence.DAILY -> 24L * 60L * 60L * 1000L
                RecurringCadence.WEEKLY -> 7L * 24L * 60L * 60L * 1000L
                RecurringCadence.MONTHLY -> 30L * 24L * 60L * 60L * 1000L
                RecurringCadence.YEARLY -> 365L * 24L * 60L * 60L * 1000L
            }
        }
    }
