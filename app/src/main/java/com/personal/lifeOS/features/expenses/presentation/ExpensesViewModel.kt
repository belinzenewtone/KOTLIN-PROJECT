package com.personal.lifeOS.features.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.core.telemetry.ImportHealthSummary
import com.personal.lifeOS.features.expenses.domain.model.SpendingSummary
import com.personal.lifeOS.features.expenses.domain.model.Transaction
import com.personal.lifeOS.features.expenses.domain.model.TransactionFilter
import com.personal.lifeOS.features.expenses.domain.usecase.AddTransactionUseCase
import com.personal.lifeOS.features.expenses.domain.usecase.DeleteTransactionUseCase
import com.personal.lifeOS.features.expenses.domain.usecase.GetSpendingSummaryUseCase
import com.personal.lifeOS.features.expenses.domain.usecase.GetTransactionsUseCase
import com.personal.lifeOS.features.expenses.domain.usecase.ImportMpesaMessagesUseCase
import com.personal.lifeOS.features.expenses.domain.usecase.ObserveImportHealthUseCase
import com.personal.lifeOS.features.expenses.domain.usecase.UpdateMerchantCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpensesUiState(
    val transactions: List<Transaction> = emptyList(),
    val summary: SpendingSummary = SpendingSummary(),
    val selectedFilter: TransactionFilter = TransactionFilter.THIS_MONTH,
    val selectedCategory: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val importResult: String? = null,
    val importHealth: ImportHealthSummary = ImportHealthSummary(),
    val showCategoryPicker: Transaction? = null,
)

@Suppress("LongParameterList")
@HiltViewModel
class ExpensesViewModel
    @Inject
    constructor(
        private val getTransactions: GetTransactionsUseCase,
        private val getSpendingSummary: GetSpendingSummaryUseCase,
        private val addTransaction: AddTransactionUseCase,
        private val deleteTransaction: DeleteTransactionUseCase,
        private val updateMerchantCategory: UpdateMerchantCategoryUseCase,
        private val importMpesaMessages: ImportMpesaMessagesUseCase,
        private val observeImportHealthUseCase: ObserveImportHealthUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ExpensesUiState())
        val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()
        private var transactionsObservationJob: Job? = null

        init {
            loadTransactions()
            loadSummary()
            observeImportHealth()
        }

        fun setFilter(filter: TransactionFilter) {
            _uiState.update { it.copy(selectedFilter = filter, selectedCategory = null) }
            loadTransactions()
        }

        fun filterByCategory(category: String?) {
            _uiState.update { it.copy(selectedCategory = category) }
            loadTransactions()
        }

        fun showAddDialog() {
            _uiState.update { it.copy(showAddDialog = true) }
        }

        fun hideAddDialog() {
            _uiState.update { it.copy(showAddDialog = false) }
        }

        fun showCategoryPicker(transaction: Transaction) {
            _uiState.update { it.copy(showCategoryPicker = transaction) }
        }

        fun hideCategoryPicker() {
            _uiState.update { it.copy(showCategoryPicker = null) }
        }

        fun addManualTransaction(
            amount: Double,
            merchant: String,
            category: String,
            date: Long = System.currentTimeMillis(),
        ) {
            viewModelScope.launch {
                try {
                    addTransaction(
                        Transaction(
                            amount = amount,
                            merchant = merchant,
                            category = category,
                            date = date,
                            source = "Manual",
                        ),
                    )
                    _uiState.update { it.copy(showAddDialog = false) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Failed to add transaction: ${e.message}") }
                }
            }
        }

        fun importSms(smsBody: String) {
            viewModelScope.launch {
                try {
                    addTransaction.fromSms(smsBody)
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Failed to import SMS: ${e.message}") }
                }
            }
        }

        fun delete(transaction: Transaction) {
            viewModelScope.launch {
                try {
                    deleteTransaction(transaction)
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Failed to delete: ${e.message}") }
                }
            }
        }

        fun recategorize(
            transaction: Transaction,
            newCategory: String,
        ) {
            viewModelScope.launch {
                try {
                    // Update the merchant mapping so future transactions auto-categorize
                    updateMerchantCategory(transaction.merchant, newCategory)
                    // Update this specific transaction
                    addTransaction(transaction.copy(category = newCategory))
                    hideCategoryPicker()
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Failed to update category: ${e.message}") }
                }
            }
        }

        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }

        private fun loadTransactions() {
            transactionsObservationJob?.cancel()
            val state = _uiState.value
            val flow =
                if (state.selectedCategory != null) {
                    getTransactions.byCategory(state.selectedCategory)
                } else {
                    getTransactions(state.selectedFilter)
                }

            transactionsObservationJob =
                flow
                    .onEach { txList ->
                        _uiState.update { it.copy(transactions = txList, isLoading = false) }
                    }
                    .catch { e ->
                        _uiState.update { it.copy(error = e.message, isLoading = false) }
                    }
                    .launchIn(viewModelScope)
        }

        fun showImportDialog() {
            _uiState.update { it.copy(showImportDialog = true) }
        }

        fun hideImportDialog() {
            _uiState.update { it.copy(showImportDialog = false, importResult = null) }
        }

        fun importSmsMessages(daysBack: Int) {
            viewModelScope.launch {
                try {
                    _uiState.update { it.copy(importResult = "Scanning SMS...") }
                    val summary = importMpesaMessages(daysBack)
                    val message =
                        when {
                            !summary.permissionGranted -> "SMS permission is required before importing MPESA messages"
                            summary.imported > 0 ->
                                "Imported ${summary.imported} transactions • " +
                                    "${summary.duplicates} duplicates • " +
                                    "${summary.pendingReview} pending review"
                            summary.scannedMessages == 0 -> "No MPESA messages found in the selected period"
                            else ->
                                "No new imports • ${summary.duplicates} duplicates • " +
                                    "${summary.parseFailed} parse failed"
                        }
                    _uiState.update {
                        it.copy(
                            importResult = message,
                            showImportDialog = false,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(importResult = "Import failed: ${e.message}") }
                }
            }
        }

        private fun loadSummary() {
            getSpendingSummary()
                .onEach { summary ->
                    _uiState.update { it.copy(summary = summary) }
                }
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .launchIn(viewModelScope)
        }

        private fun observeImportHealth() {
            observeImportHealthUseCase()
                .onEach { health ->
                    _uiState.update { it.copy(importHealth = health) }
                }
                .catch { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to load import health") }
                }
                .launchIn(viewModelScope)
        }
    }
