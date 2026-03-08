package com.personal.lifeOS.features.income.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.income.domain.model.IncomeRecord
import com.personal.lifeOS.features.income.domain.repository.IncomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
        private val repository: IncomeRepository,
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
                repository.addIncome(
                    IncomeRecord(
                        source = source,
                        amount = amount,
                        note = state.noteInput.trim(),
                    ),
                )
                hideDialog()
            }
        }

        fun deleteIncome(id: Long) {
            viewModelScope.launch {
                repository.deleteIncome(id)
            }
        }

        private fun loadData() {
            repository.getIncomes()
                .onEach { incomes ->
                    val monthStart = DateUtils.monthStartMillis()
                    val monthEnd = DateUtils.monthEndMillis()
                    val monthlyTotal =
                        incomes
                            .filter { it.date in monthStart..monthEnd }
                            .sumOf { it.amount }

                    _uiState.update {
                        it.copy(
                            records = incomes,
                            monthTotal = monthlyTotal,
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }
