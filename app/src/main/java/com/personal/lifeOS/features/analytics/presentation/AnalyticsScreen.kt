package com.personal.lifeOS.features.analytics.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.features.analytics.domain.model.AnalyticsPeriod
import com.personal.lifeOS.ui.theme.AppSpacing
import com.personal.lifeOS.ui.theme.BackgroundDark
import com.personal.lifeOS.ui.theme.Primary
import com.personal.lifeOS.ui.theme.TextSecondary

@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

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
        Text("Analytics", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Your life metrics at a glance",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )

        if (state.isLoading) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Primary)
            }
            return@Column
        }

        AnalyticsMetricRows(data = state.data)
        ProductivityCard(score = state.data.productivityScore)
        PeriodSelector(
            selected = state.selectedPeriod,
            onSelect = { viewModel.setPeriod(it) },
        )

        val spendingData =
            when (state.selectedPeriod) {
                AnalyticsPeriod.WEEK -> state.data.weeklySpending
                AnalyticsPeriod.MONTH -> state.data.monthlySpending
            }

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
