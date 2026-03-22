package com.personal.lifeOS.feature.finance.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.core.telemetry.ImportHealthSummary
import com.personal.lifeOS.core.ui.model.SyncStatusUiModel
import com.personal.lifeOS.feature.finance.domain.model.FinanceTransaction
import com.personal.lifeOS.feature.finance.domain.model.FinanceSnapshot
import com.personal.lifeOS.feature.finance.domain.model.toExpenseTransaction
import com.personal.lifeOS.feature.finance.domain.usecase.BuildFinanceSummaryUseCase
import com.personal.lifeOS.feature.finance.domain.usecase.FilterFinanceTransactionsUseCase
import com.personal.lifeOS.feature.finance.domain.usecase.ObserveFinanceSnapshotUseCase
import com.personal.lifeOS.features.expenses.domain.repository.FulizaLoanRepository
import com.personal.lifeOS.features.expenses.domain.usecase.AddTransactionUseCase
import com.personal.lifeOS.features.expenses.domain.usecase.DeleteTransactionUseCase
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class FinanceViewModel
    @Inject
    constructor(
        private val observeFinanceSnapshotUseCase: ObserveFinanceSnapshotUseCase,
        private val buildFinanceSummaryUseCase: BuildFinanceSummaryUseCase,
        private val filterFinanceTransactionsUseCase: FilterFinanceTransactionsUseCase,
        private val addTransactionUseCase: AddTransactionUseCase,
        private val deleteTransactionUseCase: DeleteTransactionUseCase,
        private val updateMerchantCategoryUseCase: UpdateMerchantCategoryUseCase,
        private val importMpesaMessagesUseCase: ImportMpesaMessagesUseCase,
        private val observeImportHealthUseCase: ObserveImportHealthUseCase,
        private val fulizaLoanRepository: FulizaLoanRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(FinanceUiState())
        val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

        private var latestSnapshot = FinanceSnapshot()
        private var latestImportHealth = ImportHealthSummary()

        init {
            observeFinanceSnapshot()
            observeImportHealth()
            observeFulizaDebt()
        }

        fun onEvent(event: FinanceUiEvent) {
            when (event) {
                is FinanceUiEvent.SelectFilter -> {
                    _uiState.update { it.copy(selectedFilter = event.filter) }
                    refreshDerivedState()
                }
                FinanceUiEvent.ShowAddDialog -> _uiState.update { it.copy(showAddDialog = true) }
                FinanceUiEvent.HideAddDialog -> _uiState.update { it.copy(showAddDialog = false) }
                is FinanceUiEvent.AddManualTransaction -> addManualTransaction(event)
                is FinanceUiEvent.DeleteTransaction -> deleteTransaction(event.transaction)
                is FinanceUiEvent.ShowCategoryPicker ->
                    _uiState.update { it.copy(showCategoryPicker = event.transaction) }
                FinanceUiEvent.HideCategoryPicker -> _uiState.update { it.copy(showCategoryPicker = null) }
                is FinanceUiEvent.RecategorizeTransaction -> recategorize(event.transaction, event.category)
                FinanceUiEvent.ShowImportDialog -> _uiState.update { it.copy(showImportDialog = true) }
                FinanceUiEvent.HideImportDialog ->
                    _uiState.update { it.copy(showImportDialog = false, importResultMessage = null) }
                is FinanceUiEvent.ImportSmsMessages -> importSmsMessages(event.daysBack)
                FinanceUiEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            }
        }

        private fun observeFinanceSnapshot() {
            observeFinanceSnapshotUseCase()
                .onEach { snapshot ->
                    latestSnapshot = snapshot
                    refreshDerivedState()
                }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Failed to load finance data",
                            syncStatus = SyncStatusUiModel.FAILED,
                        )
                    }
                }
                .launchIn(viewModelScope)
        }

        private fun observeImportHealth() {
            observeImportHealthUseCase()
                .onEach { health ->
                    latestImportHealth = health
                    refreshDerivedState()
                }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            errorMessage = throwable.message ?: "Failed to load import health",
                            syncStatus = SyncStatusUiModel.FAILED,
                        )
                    }
                }
                .launchIn(viewModelScope)
        }

        private fun refreshDerivedState() {
            _uiState.update { current ->
                current.copy(
                    summary = buildFinanceSummaryUseCase(latestSnapshot),
                    recentTransactions = buildVisibleTransactions(current),
                    importHealth = latestImportHealth.toUiModel(current.importResultMessage),
                    syncStatus =
                        if (current.errorMessage.isNullOrBlank()) {
                            SyncStatusUiModel.SYNCED
                        } else {
                            SyncStatusUiModel.FAILED
                        },
                    isLoading = false,
                )
            }
        }

        private fun buildVisibleTransactions(current: FinanceUiState): List<FinanceTransaction> {
            return filterFinanceTransactionsUseCase(
                snapshot = latestSnapshot,
                filter = current.selectedFilter,
            ).take(MAX_VISIBLE_TRANSACTIONS)
        }

        private fun addManualTransaction(event: FinanceUiEvent.AddManualTransaction) {
            viewModelScope.launch {
                runCatching {
                    val transaction =
                        FinanceTransaction(
                            amount = event.amount,
                            merchant = event.merchant,
                            category = event.category,
                            date = event.date,
                            source = "Manual",
                        )
                    addTransactionUseCase(
                        transaction.toExpenseTransaction(),
                    )
                }.onSuccess {
                    _uiState.update { it.copy(showAddDialog = false, errorMessage = null) }
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            errorMessage = "Failed to add transaction: ${throwable.message}",
                            syncStatus = SyncStatusUiModel.FAILED,
                        )
                    }
                }
            }
        }

        private fun deleteTransaction(transaction: FinanceTransaction) {
            viewModelScope.launch {
                runCatching { deleteTransactionUseCase(transaction.toExpenseTransaction()) }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                errorMessage = "Failed to delete transaction: ${throwable.message}",
                                syncStatus = SyncStatusUiModel.FAILED,
                            )
                        }
                    }
            }
        }

        private fun recategorize(
            transaction: FinanceTransaction,
            category: String,
        ) {
            viewModelScope.launch {
                runCatching {
                    updateMerchantCategoryUseCase(transaction.merchant, category)
                    addTransactionUseCase(transaction.copy(category = category).toExpenseTransaction())
                }.onSuccess {
                    _uiState.update { it.copy(showCategoryPicker = null, errorMessage = null) }
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            errorMessage = "Failed to update category: ${throwable.message}",
                            syncStatus = SyncStatusUiModel.FAILED,
                        )
                    }
                }
            }
        }

        private fun observeFulizaDebt() {
            fulizaLoanRepository.observeNetOutstanding()
                .onEach { outstanding ->
                    _uiState.update {
                        it.copy(
                            fulizaNetOutstandingKes = outstanding,
                            showFulizaBanner = outstanding > 0.0,
                        )
                    }
                }
                .catch { /* non-fatal: Fuliza tracking is additive */ }
                .launchIn(viewModelScope)
        }

        private fun importSmsMessages(daysBack: Int) {
            viewModelScope.launch {
                _uiState.update { it.copy(importResultMessage = "Scanning SMS...") }
                runCatching { importMpesaMessagesUseCase(daysBack) }
                    .onSuccess { summary ->
                        val message = summary.toResultMessage()
                        _uiState.update {
                            it.copy(
                                importResultMessage = message,
                                showImportDialog = false,
                                errorMessage = null,
                            )
                        }
                        refreshDerivedState()
                    }.onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                importResultMessage = "Import failed: ${throwable.message}",
                                syncStatus = SyncStatusUiModel.FAILED,
                            )
                        }
                        refreshDerivedState()
                    }
            }
        }

        private companion object {
            const val MAX_VISIBLE_TRANSACTIONS = 20
        }
    }

private fun com.personal.lifeOS.platform.sms.background.MpesaHistoricalImportSummary.toResultMessage(): String {
    return when {
        !permissionGranted -> "SMS permission is required before importing MPESA messages"
        imported > 0 ->
            "Imported $imported transactions • $duplicates duplicates • $pendingReview pending review"
        scannedMessages == 0 -> "No MPESA messages found in the selected period"
        else -> "No new imports • $duplicates duplicates • $parseFailed parse failed"
    }
}
