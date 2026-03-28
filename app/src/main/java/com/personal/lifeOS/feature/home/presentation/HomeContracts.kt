package com.personal.lifeOS.feature.home.presentation

import com.personal.lifeOS.core.datastore.FeatureFlag
import com.personal.lifeOS.core.ui.model.FreshnessUiModel
import com.personal.lifeOS.core.ui.model.ImportHealthUiModel
import com.personal.lifeOS.core.ui.model.SyncStatusUiModel
import com.personal.lifeOS.core.ui.model.UpdateNudgeUiModel
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.dashboard.presentation.DashboardUiState

data class HomeUiState(
    val greeting: String,
    val dateLabel: String,
    val todaySpending: Double,
    val weekSpending: Double,
    val monthSpending: Double,
    val monthNet: Double = 0.0,
    val monthIncome: Double = 0.0,
    val pendingTaskCount: Int,
    val completedTodayCount: Int,
    val nextEventTitle: String?,
    val nextEventTimeLabel: String?,
    val topInsight: String?,
    val importHealth: ImportHealthUiModel,
    val syncStatus: SyncStatusUiModel,
    val syncFreshness: FreshnessUiModel?,
    val weeklyRitual: HomeWeeklyRitualUiModel?,
    val quickActions: List<HomeQuickActionUiModel>,
    val updateNudge: UpdateNudgeUiModel?,
    val isLoading: Boolean,
    val errorMessage: String?,
)

data class HomeWeeklyRitualUiModel(
    val title: String,
    val summary: String,
    val ctaLabel: String,
)

data class HomeQuickActionUiModel(
    val label: String,
    val supportingText: String,
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
    val monthNet = data.monthIncome - data.monthSpending
    val syncFreshness =
        syncHealth.latestJobUpdatedAt?.let { latestJobUpdatedAt ->
            FreshnessUiModel(
                label = "Sync ${DateUtils.formatRelativeTime(latestJobUpdatedAt)}",
                supportingLabel = if (syncHealth.failed > 0) "Retry pending" else "Queue looks healthy",
                isStale = syncHealth.failed > 0,
            )
        } ?: lastTx?.date?.let { lastLedgerAt ->
            FreshnessUiModel(
                label = "Ledger ${DateUtils.formatRelativeTime(lastLedgerAt)}",
                supportingLabel = "Most recent finance activity",
                isStale = false,
            )
        }

    return HomeUiState(
        greeting = data.greeting,
        dateLabel = DateUtils.formatDate(System.currentTimeMillis(), "EEEE, MMM dd"),
        todaySpending = data.todaySpending,
        weekSpending = data.weekSpending,
        monthSpending = data.monthSpending,
        monthNet = monthNet,
        monthIncome = data.monthIncome,
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
                latestImportAt = lastTx?.date,
            ),
        syncStatus = deriveSyncStatus(),
        syncFreshness = syncFreshness,
        weeklyRitual =
            if (featureFlags[FeatureFlag.HOME_RITUALS] != false) {
                buildWeeklyRitual(monthNet = monthNet)
            } else {
                null
            },
        quickActions = buildQuickActions(),
        updateNudge =
            latestUpdate
                ?.takeIf { featureFlags[FeatureFlag.OTA_UPDATES] != false }
                ?.let { update ->
                    UpdateNudgeUiModel(
                        title = if (update.required) "Update required" else "Update ready",
                        summary =
                            buildString {
                                append("Version ${update.versionName ?: update.versionCode}")
                                append(" checked ${DateUtils.formatRelativeTime(update.checkedAt)}")
                            },
                        required = update.required,
                    )
                },
        isLoading = isLoading,
        errorMessage = error,
    )
}

private fun DashboardUiState.deriveSyncStatus(): SyncStatusUiModel {
    return when {
        error != null -> SyncStatusUiModel.FAILED
        syncHealth.failed > 0 -> SyncStatusUiModel.FAILED
        syncHealth.syncing > 0 -> SyncStatusUiModel.SYNCING
        syncHealth.queued > 0 -> SyncStatusUiModel.QUEUED
        else -> SyncStatusUiModel.SYNCED
    }
}

private fun DashboardUiState.buildWeeklyRitual(monthNet: Double): HomeWeeklyRitualUiModel {
    val priority =
        when {
            data.pendingTaskCount > 0 ->
                "Clear ${data.pendingTaskCount} pending task${if (data.pendingTaskCount == 1) "" else "s"} before the week closes."
            monthNet < 0.0 ->
                "Month net is ${DateUtils.formatCurrency(monthNet)}. Open Review and decide where to trim."
            data.weekSpending > data.todaySpending * 4 ->
                "Spending accelerated this week. Review the category mix before it compounds."
            else ->
                "Capture one win and one risk from this week while the context is still fresh."
        }
    return HomeWeeklyRitualUiModel(
        title = "Weekly reset",
        summary = priority,
        ctaLabel = "Open Weekly Review",
    )
}

private fun buildQuickActions(): List<HomeQuickActionUiModel> {
    return listOf(
        HomeQuickActionUiModel(label = "Tasks", supportingText = "Close open priorities"),
        HomeQuickActionUiModel(label = "Finance", supportingText = "Check imports and budgets"),
        HomeQuickActionUiModel(label = "Calendar", supportingText = "See the next commitment"),
        HomeQuickActionUiModel(label = "Assistant", supportingText = "Ask for a guided next step"),
        HomeQuickActionUiModel(label = "Review", supportingText = "Run the weekly ritual"),
    )
}
