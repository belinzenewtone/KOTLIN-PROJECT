package com.personal.lifeOS.features.income.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.features.income.domain.model.IncomeRecord
import com.personal.lifeOS.features.income.domain.usecase.AddIncomeUseCase
import com.personal.lifeOS.features.income.domain.usecase.DeleteIncomeUseCase
import com.personal.lifeOS.features.income.domain.usecase.ObserveIncomeSnapshotUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IncomeUiState(
    val records: List<IncomeRecord> = emptyList(),
    val monthTotal: Double = 0.0,
    val isLoading: Boolean = true,
    val showDialog: Boolean = false,
    val sourceInput: String = "",
    val amountInput: String = "",
    val noteInput: String = "",
    val error: String? = null,
)

@HiltViewModel
class IncomeViewModel
    @Inject
    constructor(
        private val observeIncomeSnapshotUseCase: ObserveIncomeSnapshotUseCase,
        private val addIncomeUseCase: AddIncomeUseCase,
        private val deleteIncomeUseCase: DeleteIncomeUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(IncomeUiState())
        val uiState: StateFlow<IncomeUiState> = _uiState.asStateFlow()

        init {
            loadData()
        }

        fun showAddDialog() {
            _uiState.update {
                it.copy(
                    showDialog = true,
                    sourceInput = "",
                    amountInput = "",
                    noteInput = "",
                    error = null,
                )
            }
        }

        fun hideDialog() {
            _uiState.update { it.copy(showDialog = false, error = null) }
        }

        fun setSource(value: String) {
            _uiState.update { it.copy(sourceInput = value) }
        }

        fun setAmount(value: String) {
            _uiState.update { it.copy(amountInput = value) }
        }

        fun setNote(value: String) {
            _uiState.update { it.copy(noteInput = value) }
        }

        fun saveIncome() {
            val state = _uiState.value
            val source = state.sourceInput.trim()
            val amount = state.amountInput.toDoubleOrNull()

            if (source.isBlank()) {
                _uiState.update { it.copy(error = "Source is required") }
                return
            }
            if (amount == null || amount <= 0.0) {
                _uiState.update { it.copy(error = "Enter a valid amount") }
                return
            }

            viewModelScope.launch {
                runCatching {
                    addIncomeUseCase(
                        IncomeRecord(
                            source = source,
                            amount = amount,
                            note = state.noteInput.trim(),
                        ),
                    )
                }.onSuccess {
                    hideDialog()
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(error = throwable.message ?: "Failed to add income")
                    }
                }
            }
        }

        fun deleteIncome(id: Long) {
            viewModelScope.launch {
                runCatching {
                    deleteIncomeUseCase(id)
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(error = throwable.message ?: "Failed to delete income")
                    }
                }
            }
        }

        private fun loadData() {
            observeIncomeSnapshotUseCase()
                .onEach { snapshot ->
                    _uiState.update {
                        it.copy(
                            records = snapshot.records,
                            monthTotal = snapshot.monthTotal,
                            isLoading = false,
                        )
                    }
                }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Failed to load income records",
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }
