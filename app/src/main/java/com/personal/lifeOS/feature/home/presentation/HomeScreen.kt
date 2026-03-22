package com.personal.lifeOS.feature.home.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.CalendarEventChip
import com.personal.lifeOS.core.ui.designsystem.FinanceSummaryCard
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone
import com.personal.lifeOS.core.ui.designsystem.LoadingState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.core.ui.designsystem.SyncStatusPill
import com.personal.lifeOS.core.ui.designsystem.TopBanner
import com.personal.lifeOS.core.ui.designsystem.TopBannerTone
import com.personal.lifeOS.features.dashboard.presentation.DashboardViewModel

@Composable
fun HomeScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onOpenTasks: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenLearning: () -> Unit = {}, // kept for nav compat — Learn card removed from Home
) {
    val dashboardState by viewModel.uiState.collectAsState()
    val uiState = dashboardState.toHomeUiState()

    PageScaffold(
        title = "Today",
        subtitle = uiState.dateLabel,
        topBanner = {
            uiState.errorMessage?.let {
                TopBanner(
                    message = it,
                    tone = TopBannerTone.ERROR,
                )
            }
        },
        actions = {
            // Insights icon — visible in the header, not hidden, not a nav tab
            IconButton(onClick = onOpenInsights) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = "Open Insights",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onOpenProfile) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Open profile",
                )
            }
        },
    ) {
        if (uiState.isLoading) {
            LoadingState(label = "Building your dashboard...")
            return@PageScaffold
        }

        Text(
            text = uiState.greeting,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        HomeSummaryStrip(uiState = uiState)
        HomeFocusCard(uiState = uiState, onOpenTasks = onOpenTasks)
        HomeInsightsTeaser(uiState = uiState, onOpenInsights = onOpenInsights)
        HomeCalendarCard(uiState = uiState, onOpenCalendar = onOpenCalendar)

        HomeAssistantCard(uiState = uiState, onOpenAssistant = onOpenAssistant)
        HomeSyncRow(uiState = uiState)
    }
}

@Composable
private fun HomeSummaryStrip(uiState: HomeUiState) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FinanceSummaryCard(
            title = "Today",
            amount = uiState.todaySpending,
            modifier = Modifier.width(180.dp),
        )
        FinanceSummaryCard(
            title = "This Week",
            amount = uiState.weekSpending,
            modifier = Modifier.width(180.dp),
        )
        FinanceSummaryCard(
            title = "This Month",
            amount = uiState.monthSpending,
            modifier = Modifier.width(180.dp),
        )
        FinanceSummaryCard(
            title = "Net · Month",
            amount = uiState.monthNet,
            modifier = Modifier.width(180.dp),
        )
    }
}

@Composable
private fun HomeFocusCard(
    uiState: HomeUiState,
    onOpenTasks: () -> Unit,
) {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Focus",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${uiState.pendingTaskCount} pending tasks · ${uiState.completedTodayCount} completed today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onOpenTasks) {
                Text("Open Tasks")
            }
        }
    }
}

/**
 * Insights teaser card — sits naturally in the Home feed so users discover Insights
 * without it being a full tab. Tapping the card or the "View all" button opens the
 * Insights screen.
 */
@Composable
private fun HomeInsightsTeaser(
    uiState: HomeUiState,
    onOpenInsights: () -> Unit,
    onOpenLearning: () -> Unit = {},
) {
    AppCard(elevated = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(18.dp),
                    )
                    Text(
                        text = "Insights",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = uiState.topInsight
                        ?: "Trends and patterns across your tasks, spending, and habits.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onOpenInsights) {
                Text("View all")
            }
        }
    }
}

@Composable
private fun HomeCalendarCard(
    uiState: HomeUiState,
    onOpenCalendar: () -> Unit,
) {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Next Event",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (uiState.nextEventTitle != null && uiState.nextEventTimeLabel != null) {
                CalendarEventChip(
                    title = uiState.nextEventTitle,
                    timeLabel = uiState.nextEventTimeLabel,
                )
            } else {
                Text(
                    text = "No upcoming event scheduled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onOpenCalendar) {
                Text("Open Calendar")
            }
        }
    }
}

@Composable
private fun HomeAssistantCard(
    uiState: HomeUiState,
    onOpenAssistant: () -> Unit,
) {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Assistant",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Ask about your day, tasks, or finances.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onOpenAssistant) {
                Text("Open Assistant")
            }
        }
    }
}

@Composable
private fun HomeSyncRow(uiState: HomeUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Sync",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SyncStatusPill(status = uiState.syncStatus)
    }
}

// HomeLearningCard removed — Learn section not yet built; dead card removed to keep Home clean.
