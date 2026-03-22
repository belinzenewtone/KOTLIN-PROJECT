package com.personal.lifeOS.feature.finance.presentation

import com.personal.lifeOS.core.telemetry.ImportHealthSummary
import com.personal.lifeOS.core.ui.model.ImportHealthUiModel
import com.personal.lifeOS.core.ui.model.SyncStatusUiModel
import com.personal.lifeOS.feature.finance.domain.model.FinanceSpendingSummary
import com.personal.lifeOS.feature.finance.domain.model.FinanceTransaction
import com.personal.lifeOS.feature.finance.domain.model.FinanceTransactionFilter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class FinanceUiState(
    val summary: FinanceSpendingSummary = FinanceSpendingSummary(),
    val recentTransactions: List<FinanceTransaction> = emptyList(),
    val selectedFilter: FinanceTransactionFilter = FinanceTransactionFilter.THIS_MONTH,
    val importHealth: ImportHealthUiModel = ImportHealthUiModel(),
    val syncStatus: SyncStatusUiModel = SyncStatusUiModel.UNKNOWN,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val importResultMessage: String? = null,
    val showAddDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val showCategoryPicker: FinanceTransaction? = null,
    /** Net outstanding Fuliza balance in KES. Null = unknown/loading. 0.0 = fully repaid. */
    val fulizaNetOutstandingKes: Double? = null,
    val showFulizaBanner: Boolean = false,
)

sealed interface FinanceUiEvent {
    data class SelectFilter(val filter: FinanceTransactionFilter) : FinanceUiEvent

    data object ShowAddDialog : FinanceUiEvent

    data object HideAddDialog : FinanceUiEvent

    data class AddManualTransaction(
        val amount: Double,
        val merchant: String,
        val category: String,
        val date: Long = System.currentTimeMillis(),
    ) : FinanceUiEvent

    data class DeleteTransaction(val transaction: FinanceTransaction) : FinanceUiEvent

    data class ShowCategoryPicker(val transaction: FinanceTransaction) : FinanceUiEvent

    data object HideCategoryPicker : FinanceUiEvent

    data class RecategorizeTransaction(
        val transaction: FinanceTransaction,
        val category: String,
    ) : FinanceUiEvent

    data object ShowImportDialog : FinanceUiEvent

    data object HideImportDialog : FinanceUiEvent

    data class ImportSmsMessages(val daysBack: Int) : FinanceUiEvent

    data object ClearError : FinanceUiEvent
}

internal fun ImportHealthSummary.toUiModel(importResultMessage: String?): ImportHealthUiModel {
    return ImportHealthUiModel(
        pendingReviewCount = pending,
        duplicateCount = duplicate,
        parseFailureCount = parseFailed,
        lastImportSummary =
            importResultMessage
                ?: latestImportAt?.let { lastImportTime ->
                    val formatted =
                        DateTimeFormatter.ofPattern("MMM dd, HH:mm")
                            .format(
                                Instant.ofEpochMilli(lastImportTime)
                                    .atZone(ZoneId.systemDefault()),
                            )
                    "Last import $formatted"
                },
    )
}
