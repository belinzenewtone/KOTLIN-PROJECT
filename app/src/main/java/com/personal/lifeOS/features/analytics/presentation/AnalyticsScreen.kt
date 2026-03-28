package com.personal.lifeOS.features.analytics.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.EmptyState
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone
import com.personal.lifeOS.core.ui.designsystem.LoadingState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.core.ui.designsystem.SegmentedControl
import com.personal.lifeOS.features.analytics.domain.model.AnalyticsPeriod
import com.personal.lifeOS.features.analytics.domain.model.DailySpending
import com.personal.lifeOS.ui.theme.AppSpacing

@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val periodTabs = listOf("This Week", "This Month")
    val selectedPeriodIndex = state.selectedPeriod.toSegmentIndex()
    val spendingData = state.selectedSpendingData()

    PageScaffold(
        title = "Analytics",
        subtitle = "Productivity and finance trends in one place",
        contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFloatingNav),
    ) {
        state.error?.let {
            InlineBanner(
                message = it,
                tone = InlineBannerTone.ERROR,
            )
        }

        if (state.isLoading) {
            LoadingState(label = "Loading analytics...")
            return@PageScaffold
        }

        if (!state.isLoading &&
            state.data.totalTasksCompleted == 0 &&
            state.data.totalTasksPending == 0 &&
            state.data.totalEvents == 0 &&
            state.data.totalSpentThisMonth == 0.0) {
            EmptyState(
                title = "No data yet",
                description = "Add tasks, events, or import M-Pesa messages to see insights.",
            )
            return@PageScaffold
        }

        AnalyticsLoadedContent(
            state = state,
            periodTabs = periodTabs,
            selectedPeriodIndex = selectedPeriodIndex,
            spendingData = spendingData,
            onPeriodSelected = { index -> viewModel.setPeriod(index.toAnalyticsPeriod()) },
        )
    }
}

@Composable
private fun AnalyticsLoadedContent(
    state: AnalyticsUiState,
    periodTabs: List<String>,
    selectedPeriodIndex: Int,
    spendingData: List<DailySpending>,
    onPeriodSelected: (Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 16.dp),
    ) {
        AnalyticsMetricRows(data = state.data)
        ProductivityCard(score = state.data.productivityScore)
        SegmentedControl(
            items = periodTabs,
            selectedIndex = selectedPeriodIndex,
            onSelected = onPeriodSelected,
        )

        if (spendingData.isNotEmpty() && spendingData.any { it.amount > 0 }) {
            SpendingBarChart(
                title =
                    if (state.selectedPeriod == AnalyticsPeriod.WEEK) {
                        "Weekly Spending"
                    } else {
                        "Monthly Spending"
                    },
                data = spendingData,
            )
        }

        if (state.data.monthlySpending.size > 1 && state.data.monthlySpending.any { it.amount > 0 }) {
            SpendingTrendChart(data = state.data.monthlySpending)
        }

        CategoryBreakdownCard(categories = state.data.categoryBreakdown)
    }
}

private fun AnalyticsUiState.selectedSpendingData(): List<DailySpending> {
    return if (selectedPeriod == AnalyticsPeriod.WEEK) {
        data.weeklySpending
    } else {
        data.monthlySpending
    }
}

private fun AnalyticsPeriod.toSegmentIndex(): Int {
    return if (this == AnalyticsPeriod.WEEK) 0 else 1
}

private fun Int.toAnalyticsPeriod(): AnalyticsPeriod {
    return if (this == 0) AnalyticsPeriod.WEEK else AnalyticsPeriod.MONTH
}
