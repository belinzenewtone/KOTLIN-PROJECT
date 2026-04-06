package com.personal.lifeOS.features.review.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone
import com.personal.lifeOS.core.ui.designsystem.LoadingState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.ui.theme.AppSpacing
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsState()

    PageScaffold(
        headerEyebrow = "Weekly Ritual",
        title = "Weekly Review",
        subtitle = state.weekLabel,
        onBack = onBack,
        contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFloatingNav),
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

        state.ritual?.let { ritual ->
            ReviewRitualCard(ritual = ritual)
        }
        ReviewSummaryCard(state = state)
        ReviewTaskCard(state = state)
        ReviewBulletCard(
            title = "Wins",
            items = state.wins,
        )
        ReviewBulletCard(
            title = "Risks",
            items = state.risks,
        )
        if (state.topInsights.isNotEmpty()) {
            ReviewInsightsCard(insights = state.topInsights)
        }
    }
}

@Composable
private fun ReviewRitualCard(ritual: ReviewRitualUiModel) {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = ritual.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = ritual.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReviewSummaryCard(state: ReviewUiState) {
    val ksh = NumberFormat.getCurrencyInstance(Locale("en", "KE"))
    val summaryRows =
        buildList {
            add("Total this week" to ksh.format(state.summary.totalSpend))
            add("Posture" to state.summary.postureLabel)
            add("Week delta" to state.summary.weekDeltaLabel)
            state.summary.topCategory?.let { category ->
                add("Top category" to category)
            }
        }

    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Spending",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            summaryRows.forEachIndexed { index, (label, value) ->
                ReviewStatRow(label = label, value = value)
                if (index < summaryRows.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Composable
private fun ReviewTaskCard(state: ReviewUiState) {
    val taskRows =
        listOf(
            "Completed today" to state.summary.tasksCompleted.toString(),
            "Still pending" to state.summary.tasksPending.toString(),
        )
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Tasks",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            taskRows.forEachIndexed { index, (label, value) ->
                ReviewStatRow(label = label, value = value)
                if (index < taskRows.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Composable
private fun ReviewBulletCard(
    title: String,
    items: List<String>,
) {
    AppCard(elevated = false) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            items.forEachIndexed { index, item ->
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
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (index < items.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Composable
private fun ReviewInsightsCard(insights: List<String>) {
    ReviewBulletCard(
        title = "Top Insights",
        items = insights,
    )
}

@Composable
private fun ReviewStatRow(label: String, value: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            modifier =
                Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
        )
    }
}
