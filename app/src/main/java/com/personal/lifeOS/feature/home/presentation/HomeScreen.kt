package com.personal.lifeOS.feature.home.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.CalendarEventChip
import com.personal.lifeOS.core.ui.designsystem.FinanceSummaryCard
import com.personal.lifeOS.core.ui.designsystem.ImportHealthPanel
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone
import com.personal.lifeOS.core.ui.designsystem.LoadingState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.core.ui.designsystem.SyncStatusPill
import com.personal.lifeOS.features.dashboard.presentation.DashboardViewModel

@Composable
fun HomeScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onOpenTasks: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val dashboardState by viewModel.uiState.collectAsState()
    val uiState = dashboardState.toHomeUiState()

    PageScaffold(
        title = "Today",
        subtitle = uiState.dateLabel,
        actions = {
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

        uiState.errorMessage?.let {
            InlineBanner(
                message = it,
                tone = InlineBannerTone.ERROR,
            )
        }

        Text(
            text = uiState.greeting,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        HomeSummaryStrip(uiState = uiState)
        HomeFocusCard(uiState = uiState, onOpenTasks = onOpenTasks)
        HomeCalendarCard(uiState = uiState, onOpenCalendar = onOpenCalendar)

        ImportHealthPanel(
            model = uiState.importHealth,
            onReview = onOpenFinance,
        )
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
                text = uiState.topInsight ?: "No new insight yet. Ask the assistant for a quick review.",
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
