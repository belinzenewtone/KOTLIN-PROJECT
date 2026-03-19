package com.personal.lifeOS.features.dashboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.ui.theme.AppSpacing
import com.personal.lifeOS.ui.theme.BackgroundDark
import com.personal.lifeOS.ui.theme.Primary
import com.personal.lifeOS.ui.theme.TextSecondary

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val data = state.data

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.ScreenHorizontal)
                .padding(top = AppSpacing.ScreenTop, bottom = AppSpacing.BottomSafe),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Section),
    ) {
        DashboardHeader(greeting = data.greeting)

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Primary)
            }
            return@Column
        }

        state.error?.let { errorMessage ->
            Text(
                text = errorMessage,
                color = TextSecondary,
            )
        }

        DashboardSpendingSummary(
            today = data.todaySpending,
            week = data.weekSpending,
            month = data.monthSpending,
        )
        ProductivityCard(
            completedTodayCount = data.completedTodayCount,
            pendingTaskCount = data.pendingTaskCount,
        )
        UpcomingEventsCard(events = data.upcomingEvents)
        WeeklySpendingCard(data = data.weeklySpendingData)
        DashboardInsightsCard(insights = data.insights)
        RecentTransactionsCard(transactions = data.recentTransactions)
    }
}
