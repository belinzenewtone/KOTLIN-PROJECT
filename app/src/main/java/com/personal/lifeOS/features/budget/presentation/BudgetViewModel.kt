package com.personal.lifeOS.features.budget.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.features.budget.domain.model.Budget
import com.personal.lifeOS.features.budget.domain.model.BudgetPeriod
import com.personal.lifeOS.features.budget.domain.model.BudgetProgress
import com.personal.lifeOS.features.budget.domain.usecase.AddBudgetUseCase
import com.personal.lifeOS.features.budget.domain.usecase.DeleteBudgetUseCase
import com.personal.lifeOS.features.budget.domain.usecase.ObserveBudgetProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetUiState(
    val budgets: List<BudgetProgress> = emptyList(),
    val isLoading: Boolean = true,
    val showDialog: Boolean = false,
    val categoryInput: String = "",
    val limitInput: String = "",
    val periodInput: BudgetPeriod = BudgetPeriod.MONTHLY,
    val error: String? = null,
)

@HiltViewModel
class BudgetViewModel
    @Inject
    constructor(
        private val observeBudgetProgressUseCase: ObserveBudgetProgressUseCase,
        private val addBudgetUseCase: AddBudgetUseCase,
        private val deleteBudgetUseCase: DeleteBudgetUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(BudgetUiState())
        val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

        init {
            loadData()
        }

        fun showAddDialog() {
            _uiState.update {
                it.copy(
                    showDialog = true,
                    categoryInput = "",
                    limitInput = "",
                    periodInput = BudgetPeriod.MONTHLY,
                    error = null,
                )
            }
        }

        fun hideDialog() {
            _uiState.update { it.copy(showDialog = false, error = null) }
        }

        fun setCategory(value: String) {
            _uiState.update { it.copy(categoryInput = value) }
        }

        fun setLimit(value: String) {
            _uiState.update { it.copy(limitInput = value) }
        }

        fun setPeriod(value: BudgetPeriod) {
            _uiState.update { it.copy(periodInput = value) }
        }

        fun saveBudget() {
            val state = _uiState.value
            val category = state.categoryInput.trim()
            val limit = state.limitInput.toDoubleOrNull()

            if (category.isBlank()) {
                _uiState.update { it.copy(error = "Category is required") }
                return
            }
            if (limit == null || limit <= 0.0) {
                _uiState.update { it.copy(error = "Enter a valid budget amount") }
                return
            }

            viewModelScope.launch {
                runCatching {
                    addBudgetUseCase(
                        Budget(
                            category = category,
                            limitAmount = limit,
                            period = state.periodInput,
                        ),
                    )
                }.onSuccess {
                    hideDialog()
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(error = throwable.message ?: "Failed to save budget")
                    }
                }
            }
        }

        fun deleteBudget(id: Long) {
            viewModelScope.launch {
                runCatching {
                    deleteBudgetUseCase(id)
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(error = throwable.message ?: "Failed to delete budget")
                    }
                }
            }
        }

        private fun loadData() {
            observeBudgetProgressUseCase()
                .onEach { progress ->
                    _uiState.update {
                        it.copy(
                            budgets = progress,
                            isLoading = false,
                        )
                    }
                }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Failed to load budgets",
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }
