package com.personal.lifeOS.feature.finance.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.personal.lifeOS.core.datastore.FeatureFlag
import com.personal.lifeOS.core.datastore.FeatureFlagStore
import com.personal.lifeOS.core.preferences.AppSettingsStore
import com.personal.lifeOS.core.telemetry.HealthDiagnosticsRepository
import com.personal.lifeOS.core.telemetry.ImportHealthSummary
import com.personal.lifeOS.core.ui.model.SyncStatusUiModel
import com.personal.lifeOS.feature.finance.domain.model.FinanceTransaction
import com.personal.lifeOS.feature.finance.domain.model.FinanceSnapshot
import com.personal.lifeOS.feature.finance.domain.model.toExpenseTransaction
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.feature.finance.domain.repository.FinanceRepository
import com.personal.lifeOS.feature.finance.domain.usecase.BuildFinanceSummaryUseCase
import com.personal.lifeOS.feature.finance.domain.usecase.ObserveFinanceSnapshotUseCase
import com.personal.lifeOS.features.expenses.domain.repository.FulizaLoanRepository
import com.personal.lifeOS.features.expenses.domain.usecase.AddTransactionUseCase
import com.personal.lifeOS.features.expenses.domain.usecase.DeleteTransactionUseCase
import com.personal.lifeOS.features.expenses.domain.usecase.ImportMpesaMessagesUseCase
import com.personal.lifeOS.features.expenses.domain.usecase.ObserveImportHealthUseCase
import com.personal.lifeOS.features.expenses.domain.usecase.UpdateMerchantCategoryUseCase
import com.personal.lifeOS.platform.sms.background.MpesaHistoricalImportSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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
        private val financeRepository: FinanceRepository,
        private val addTransactionUseCase: AddTransactionUseCase,
        private val deleteTransactionUseCase: DeleteTransactionUseCase,
        private val updateMerchantCategoryUseCase: UpdateMerchantCategoryUseCase,
        private val importMpesaMessagesUseCase: ImportMpesaMessagesUseCase,
        private val observeImportHealthUseCase: ObserveImportHealthUseCase,
        private val fulizaLoanRepository: FulizaLoanRepository,
        private val healthDiagnosticsRepository: HealthDiagnosticsRepository,
        private val featureFlagStore: FeatureFlagStore,
        private val appSettingsStore: AppSettingsStore,
        private val transactionDao: TransactionDao,
        private val authSessionStore: AuthSessionStore,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(FinanceUiState())
        val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

        @OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        val pagedTransactions: Flow<PagingData<FinanceTransaction>> =
            _uiState
                .map { Pair(it.selectedFilter, it.searchQuery) }
                .distinctUntilChanged()
                .debounce(300L)
                .flatMapLatest { (filter, query) ->
                    financeRepository.pagedTransactions(filter, query)
                }
                .cachedIn(viewModelScope)

        private var latestSnapshot = FinanceSnapshot()
        private var latestImportHealth = ImportHealthSummary()
        private var latestSyncUpdatedAt: Long? = null
        private var latestFulizaLimitKes: Double? = null
        private var importResultAutoClearJob: Job? = null

        init {
            loadFeatureFlags()
            observeFinanceSnapshot()
            observeImportHealth()
            observeFulizaLimit()
            observeFulizaDebt()
            observeSyncHealth()
            observeUncategorizedCount()
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
                FinanceUiEvent.DismissFulizaLimitDialog ->
                    _uiState.update { it.copy(showFulizaLimitDialog = false) }
                is FinanceUiEvent.SaveFulizaLimit -> saveFulizaLimit(event.limitKes)
                FinanceUiEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
                is FinanceUiEvent.UpdateSearchQuery ->
                    _uiState.update { it.copy(searchQuery = event.query) }
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
                val summary = buildFinanceSummaryUseCase(latestSnapshot)
                val syncStatus =
                    when {
                        current.errorMessage.isNullOrBlank().not() -> SyncStatusUiModel.FAILED
                        latestSyncUpdatedAt == null && latestImportHealth.latestImportAt == null -> SyncStatusUiModel.LOCAL_ONLY
                        latestImportHealth.parseFailed > 0 -> SyncStatusUiModel.FAILED
                        else -> SyncStatusUiModel.SYNCED
                    }
                val monthFeesTotal = latestSnapshot.transactions
                    .filter { it.category.uppercase() in FEE_CATEGORIES }
                    .sumOf { it.amount }
                current.copy(
                    summary = summary,
                    importHealth = latestImportHealth.toUiModel(current.lastImportRunSummary),
                    syncStatus = syncStatus,
                    freshness = buildFinanceFreshness(latestSnapshot, latestImportHealth, latestSyncUpdatedAt),
                    reviewQueueSummary = buildReviewQueueSummary(latestImportHealth),
                    budgetGuardrail = buildBudgetGuardrail(summary, latestSnapshot.totalBudgetLimit),
                    exportNudge = buildExportNudge(summary),
                    totalMonthBudget = latestSnapshot.totalBudgetLimit,
                    monthFeesTotal = monthFeesTotal,
                    isLoading = false,
                )
            }
        }

        private fun observeUncategorizedCount() {
            viewModelScope.launch {
                val userId = authSessionStore.getUserId()
                transactionDao.getUncategorizedCount(userId)
                    .onEach { count ->
                        _uiState.update { it.copy(uncategorizedCount = count) }
                    }
                    .catch { /* non-fatal — uncategorized banner is cosmetic */ }
                    .launchIn(viewModelScope)
            }
        }

        private fun observeSyncHealth() {
            healthDiagnosticsRepository.observeSyncHealth()
                .onEach { summary ->
                    latestSyncUpdatedAt = summary.latestJobUpdatedAt
                    _uiState.update { current ->
                        current.copy(
                            syncStatus =
                                when {
                                    summary.failed > 0 -> SyncStatusUiModel.FAILED
                                    summary.syncing > 0 -> SyncStatusUiModel.SYNCING
                                    summary.queued > 0 -> SyncStatusUiModel.QUEUED
                                    summary.latestJobUpdatedAt != null -> SyncStatusUiModel.SYNCED
                                    else -> current.syncStatus
                                },
                        )
                    }
                    refreshDerivedState()
                }.catch { /* non-fatal: finance still renders with local state */ }
                .launchIn(viewModelScope)
        }

        private fun loadFeatureFlags() {
            viewModelScope.launch {
                runCatching { featureFlagStore.isEnabled(FeatureFlag.FINANCE_HEALTH_V2) }
                    .onSuccess { enabled ->
                        _uiState.update { it.copy(enhancedUiEnabled = enabled) }
                    }
            }
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
                    maybeShowFulizaLimitDialog(hasFulizaSignal = outstanding > 0.0)
                }
                .catch { /* non-fatal: Fuliza tracking is additive */ }
                .launchIn(viewModelScope)

            fulizaLoanRepository.observeOpenLoans()
                .onEach { loans ->
                    val openCount = loans.count { it.status == "OPEN" || it.status == "PARTIALLY_REPAID" }
                    _uiState.update { it.copy(fulizaOpenCount = openCount) }
                    maybeShowFulizaLimitDialog(hasFulizaSignal = openCount > 0)
                }
                .catch { /* non-fatal: Fuliza tracking is additive */ }
                .launchIn(viewModelScope)
        }

        private fun observeFulizaLimit() {
            appSettingsStore.fulizaLimitKesFlow()
                .onEach { limit ->
                    latestFulizaLimitKes = limit
                    _uiState.update {
                        it.copy(
                            fulizaLimitKes = limit,
                            showFulizaLimitDialog = if (limit != null) false else it.showFulizaLimitDialog,
                        )
                    }
                }
                .catch { /* non-fatal */ }
                .launchIn(viewModelScope)
        }

        private fun maybeShowFulizaLimitDialog(hasFulizaSignal: Boolean) {
            if (!hasFulizaSignal) return
            if (latestFulizaLimitKes != null) return
            _uiState.update { state ->
                if (state.showFulizaLimitDialog) state else state.copy(showFulizaLimitDialog = true)
            }
        }

        private fun importSmsMessages(daysBack: Int) {
            viewModelScope.launch {
                importResultAutoClearJob?.cancel()
                _uiState.update { it.copy(importResultMessage = "Scanning SMS...") }
                runCatching { importMpesaMessagesUseCase(daysBack) }
                    .onSuccess { summary ->
                        val message = summary.toResultMessage()
                        _uiState.update {
                            it.copy(
                                importResultMessage = message,
                                lastImportRunSummary = summary.toPersistentHealthSummary(),
                                showImportDialog = false,
                                errorMessage = null,
                            )
                        }
                        scheduleImportResultAutoDismiss(message)
                        maybeShowFulizaLimitDialog(hasFulizaSignal = summary.fulizaSignalsDetected > 0)
                        refreshDerivedState()
                    }.onFailure { throwable ->
                        val errorMessage = "Import failed: ${throwable.message}"
                        _uiState.update {
                            it.copy(
                                importResultMessage = errorMessage,
                                syncStatus = SyncStatusUiModel.FAILED,
                            )
                        }
                        scheduleImportResultAutoDismiss(errorMessage)
                        refreshDerivedState()
                    }
            }
        }

        private fun scheduleImportResultAutoDismiss(message: String) {
            importResultAutoClearJob?.cancel()
            importResultAutoClearJob =
                viewModelScope.launch {
                    delay(7000L)
                    _uiState.update { current ->
                        if (current.importResultMessage == message) {
                            current.copy(
                                importResultMessage = null,
                                lastImportRunSummary = null,
                            )
                        } else {
                            current
                        }
                    }
                }
        }

        private fun saveFulizaLimit(limitKes: Double) {
            viewModelScope.launch {
                val normalized = limitKes.coerceAtLeast(0.0)
                runCatching { appSettingsStore.setFulizaLimitKes(normalized) }
                    .onSuccess {
                        _uiState.update { it.copy(showFulizaLimitDialog = false, fulizaLimitKes = normalized) }
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                errorMessage = "Failed to save Fuliza limit: ${throwable.message}",
                                syncStatus = SyncStatusUiModel.FAILED,
                            )
                        }
                    }
            }
        }

        companion object {
            /** Fee-like categories used to compute the month fees total.
             *  Defined once as a constant — avoids recreating the Set on every snapshot update. */
            private val FEE_CATEGORIES = setOf(
                "AIRTIME", "FULIZA", "SUBSCRIPTIONS", "BANK CHARGES", "CHARGES", "FEES",
            )
        }
    }

private fun MpesaHistoricalImportSummary.toResultMessage(): String {
    return when {
        !permissionGranted -> "SMS permission is required before importing MPESA messages"
        imported > 0 ->
            "Imported $imported transactions | $duplicates duplicates | $pendingReview pending review"
        scannedMessages == 0 -> "No MPESA messages found in the selected period"
        else -> "No new imports | $duplicates duplicates | $parseFailed parse failed"
    }
}

private fun MpesaHistoricalImportSummary.toPersistentHealthSummary(): String {
    return when {
        !permissionGranted -> "Last run: SMS permission denied"
        scannedMessages == 0 -> "Last run: no MPESA messages found"
        else ->
            "Last run: imported $imported | duplicates $duplicates | " +
                "pending $pendingReview | parse failed $parseFailed"
    }
}
