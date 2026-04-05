package com.personal.lifeOS.feature.finance.presentation

import com.personal.lifeOS.core.telemetry.ImportHealthSummary
import com.personal.lifeOS.core.ui.model.FreshnessUiModel
import com.personal.lifeOS.core.ui.model.ImportHealthUiModel
import com.personal.lifeOS.core.ui.model.SyncStatusUiModel
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.feature.finance.domain.model.FinanceSnapshot
import com.personal.lifeOS.feature.finance.domain.model.FinanceSpendingSummary
import com.personal.lifeOS.feature.finance.domain.model.FinanceTransaction
import com.personal.lifeOS.feature.finance.domain.model.FinanceTransactionFilter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class FinanceUiState(
    val summary: FinanceSpendingSummary = FinanceSpendingSummary(),
    val selectedFilter: FinanceTransactionFilter = FinanceTransactionFilter.THIS_MONTH,
    val searchQuery: String = "",
    val importHealth: ImportHealthUiModel = ImportHealthUiModel(),
    val syncStatus: SyncStatusUiModel = SyncStatusUiModel.UNKNOWN,
    val freshness: FreshnessUiModel? = null,
    val reviewQueueSummary: String? = null,
    val budgetGuardrail: FinanceGuardrailUiModel? = null,
    val exportNudge: FinanceActionNudgeUiModel? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val importResultMessage: String? = null,
    val lastImportRunSummary: String? = null,
    val showAddDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val showFulizaLimitDialog: Boolean = false,
    val fulizaLimitKes: Double? = null,
    val showCategoryPicker: FinanceTransaction? = null,
    val fulizaNetOutstandingKes: Double? = null,
    val showFulizaBanner: Boolean = false,
    val fulizaOpenCount: Int = 0,
    val totalMonthBudget: Double = 0.0,
    val enhancedUiEnabled: Boolean = true,
)

data class FinanceGuardrailUiModel(
    val title: String,
    val message: String,
)

data class FinanceActionNudgeUiModel(
    val title: String,
    val summary: String,
    val actionLabel: String,
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

    data object DismissFulizaLimitDialog : FinanceUiEvent

    data class SaveFulizaLimit(val limitKes: Double) : FinanceUiEvent

    data object ClearError : FinanceUiEvent

    data class UpdateSearchQuery(val query: String) : FinanceUiEvent
}

internal fun ImportHealthSummary.toUiModel(lastImportRunSummary: String?): ImportHealthUiModel {
    val totalImported = imported + recovered
    return ImportHealthUiModel(
        importedCount = totalImported,
        pendingReviewCount = pending,
        duplicateCount = duplicate,
        parseFailureCount = parseFailed,
        recoveredCount = recovered,
        lastImportSummary =
            lastImportRunSummary
                ?: latestImportAt?.let { lastImportTime ->
                    val formatted =
                        DateTimeFormatter.ofPattern("MMM dd, HH:mm")
                            .format(
                                Instant.ofEpochMilli(lastImportTime)
                                    .atZone(ZoneId.systemDefault()),
                            )
                    "Last import $formatted"
                },
        latestImportAt = latestImportAt,
    )
}

internal fun buildFinanceFreshness(
    snapshot: FinanceSnapshot,
    importHealth: ImportHealthSummary,
    lastSyncAt: Long?,
): FreshnessUiModel? {
    val timestamp = importHealth.latestImportAt ?: lastSyncAt ?: snapshot.transactions.maxOfOrNull { it.date } ?: return null
    val supporting =
        when {
            importHealth.pending > 0 -> "${importHealth.pending} items waiting for review"
            importHealth.parseFailed > 0 -> "${importHealth.parseFailed} import issue${if (importHealth.parseFailed == 1) "" else "s"}"
            else -> "Recent ledger activity is visible"
        }
    val staleReference = System.currentTimeMillis() - timestamp > 48L * 60L * 60L * 1000L
    return FreshnessUiModel(
        label = "Updated ${DateUtils.formatRelativeTime(timestamp)}",
        supportingLabel = supporting,
        isStale = staleReference,
    )
}

internal fun buildReviewQueueSummary(importHealth: ImportHealthSummary): String? {
    return when {
        importHealth.pending > 0 ->
            "${importHealth.pending} transaction${if (importHealth.pending == 1) "" else "s"} waiting for review"
        importHealth.parseFailed > 0 ->
            "${importHealth.parseFailed} import issue${if (importHealth.parseFailed == 1) "" else "s"} need attention"
        else -> null
    }
}

internal fun buildBudgetGuardrail(
    summary: FinanceSpendingSummary,
    totalMonthBudget: Double,
): FinanceGuardrailUiModel? {
    if (totalMonthBudget <= 0.0 || summary.monthTotal <= 0.0) return null
    val spendRatio = summary.monthTotal / totalMonthBudget
    return when {
        spendRatio >= 1.0 ->
            FinanceGuardrailUiModel(
                title = "Budget guardrail",
                message = "You are ${DateUtils.formatCurrency(summary.monthTotal - totalMonthBudget)} over the current monthly budget.",
            )
        spendRatio >= 0.8 ->
            FinanceGuardrailUiModel(
                title = "Budget guardrail",
                message = "You have used ${(spendRatio * 100).toInt()}% of the monthly budget. Review the top category before month end.",
            )
        else -> null
    }
}

internal fun buildExportNudge(summary: FinanceSpendingSummary): FinanceActionNudgeUiModel {
    return FinanceActionNudgeUiModel(
        title = "Exports and reports",
        summary =
            if (summary.transactionCount == 0) {
                "Prepare a clean export setup before the ledger grows."
            } else {
                "Create a CSV, JSON, or shareable report from ${summary.transactionCount} visible transactions."
            },
        actionLabel = "Open export center",
    )
}
