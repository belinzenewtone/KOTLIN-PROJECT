package com.personal.lifeOS.features.review.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone
import com.personal.lifeOS.core.ui.designsystem.LoadingState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ReviewScreen(viewModel: ReviewViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    PageScaffold(
        title = "Weekly Review",
        subtitle = state.weekLabel,
    ) {
        if (state.isLoading) {
            LoadingState(label = "Building your review…")
            return@PageScaffold
        }

        state.error?.let {
            InlineBanner(message = it, tone = InlineBannerTone.ERROR)
        }

        Text(
            text = state.greeting,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // ── Spending summary card ────────────────────────────────────────────
        ReviewSummaryCard(state = state)

        // ── Task completion card ─────────────────────────────────────────────
        ReviewTaskCard(state = state)

        // ── Insights this week ───────────────────────────────────────────────
        if (state.topInsights.isNotEmpty()) {
            ReviewInsightsCard(insights = state.topInsights)
        }
    }
}

@Composable
private fun ReviewSummaryCard(state: ReviewUiState) {
    val ksh = NumberFormat.getCurrencyInstance(Locale("en", "KE"))

    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Spending",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ReviewStatRow(
                label = "Total this week",
                value = ksh.format(state.summary.totalSpend),
            )
            state.summary.topCategory?.let { cat ->
                ReviewStatRow(label = "Top category", value = cat)
            }
        }
    }
}

@Composable
private fun ReviewTaskCard(state: ReviewUiState) {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Tasks",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ReviewStatRow(
                label = "Completed today",
                value = state.summary.tasksCompleted.toString(),
            )
            ReviewStatRow(
                label = "Still pending",
                value = state.summary.tasksPending.toString(),
            )
        }
    }
}

@Composable
private fun ReviewInsightsCard(insights: List<String>) {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Top Insights",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            insights.forEachIndexed { index, insight ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = insight,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
        )
    }
}
