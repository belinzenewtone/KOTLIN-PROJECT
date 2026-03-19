package com.personal.lifeOS.feature.home.presentation

import com.personal.lifeOS.core.ui.model.ImportHealthUiModel
import com.personal.lifeOS.core.ui.model.SyncStatusUiModel
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.dashboard.presentation.DashboardUiState

data class HomeUiState(
    val greeting: String,
    val dateLabel: String,
    val todaySpending: Double,
    val weekSpending: Double,
    val monthSpending: Double,
    val pendingTaskCount: Int,
    val completedTodayCount: Int,
    val nextEventTitle: String?,
    val nextEventTimeLabel: String?,
    val topInsight: String?,
    val importHealth: ImportHealthUiModel,
    val syncStatus: SyncStatusUiModel,
    val isLoading: Boolean,
    val errorMessage: String?,
)

sealed interface HomeUiEvent {
    data object OpenTasks : HomeUiEvent

    data object OpenFinance : HomeUiEvent

    data object OpenCalendar : HomeUiEvent

    data object OpenAssistant : HomeUiEvent

    data object OpenProfile : HomeUiEvent
}

fun DashboardUiState.toHomeUiState(): HomeUiState {
    val nextEvent = data.upcomingEvents.firstOrNull()
    val lastTx = data.recentTransactions.firstOrNull()
    return HomeUiState(
        greeting = data.greeting,
        dateLabel = DateUtils.formatDate(System.currentTimeMillis(), "EEEE, MMM dd"),
        todaySpending = data.todaySpending,
        weekSpending = data.weekSpending,
        monthSpending = data.monthSpending,
        pendingTaskCount = data.pendingTaskCount,
        completedTodayCount = data.completedTodayCount,
        nextEventTitle = nextEvent?.title,
        nextEventTimeLabel = nextEvent?.date?.let(DateUtils::formatTime),
        topInsight = data.insights.firstOrNull()?.title,
        importHealth =
            ImportHealthUiModel(
                pendingReviewCount = 0,
                duplicateCount = 0,
                parseFailureCount = 0,
                lastImportSummary =
                    lastTx?.let { tx ->
                        "Last ledger item: ${tx.merchant} (${DateUtils.formatCurrency(tx.amount)})"
                    },
            ),
        syncStatus = if (error.isNullOrBlank()) SyncStatusUiModel.SYNCED else SyncStatusUiModel.FAILED,
        isLoading = isLoading,
        errorMessage = error,
    )
}
