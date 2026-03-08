package com.personal.lifeOS.features.budget.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.budget.domain.model.Budget
import com.personal.lifeOS.features.budget.domain.model.BudgetPeriod
import com.personal.lifeOS.features.budget.domain.model.BudgetProgress
import com.personal.lifeOS.features.budget.domain.repository.BudgetRepository
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
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
        private val budgetRepository: BudgetRepository,
        private val expenseRepository: ExpenseRepository,
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
                budgetRepository.addBudget(
                    Budget(
                        category = category,
                        limitAmount = limit,
                        period = state.periodInput,
                    ),
                )
                hideDialog()
            }
        }

        fun deleteBudget(id: Long) {
            viewModelScope.launch {
                budgetRepository.deleteBudget(id)
            }
        }

        private fun loadData() {
            val monthStart = DateUtils.monthStartMillis()
            val monthEnd = DateUtils.monthEndMillis()

            combine(
                budgetRepository.getBudgets(),
                expenseRepository.getTransactionsBetween(monthStart, monthEnd),
            ) { budgets, transactions ->
                val spentByCategory =
                    transactions
                        .groupBy { it.category.trim().uppercase() }
                        .mapValues { (_, list) -> list.sumOf { tx -> tx.amount } }

                budgets.map { budget ->
                    BudgetProgress(
                        budget = budget,
                        spentAmount = spentByCategory[budget.category.trim().uppercase()] ?: 0.0,
                    )
                }
            }
                .onEach { progress ->
                    _uiState.update {
                        it.copy(
                            budgets = progress,
                            isLoading = false,
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }
