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
import com.personal.lifeOS.core.ui.designsystem.LoadingState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.core.ui.designsystem.SyncStatusPill
import com.personal.lifeOS.core.ui.designsystem.TopBanner
import com.personal.lifeOS.core.ui.designsystem.TopBannerTone
import com.personal.lifeOS.features.dashboard.presentation.DashboardViewModel
import com.personal.lifeOS.navigation.AppRoute

@Composable
fun HomeScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onOpenRoute: (String) -> Unit,
    onOpenProfile: () -> Unit,
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
            IconButton(onClick = { onOpenRoute(AppRoute.Insights) }) {
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

        HomeSyncCard(uiState = uiState)
        HomeSummaryStrip(uiState = uiState)
        HomeQuickActionsRow(
            uiState = uiState,
            onOpenRoute = onOpenRoute,
        )
        uiState.weeklyRitual?.let { ritual ->
            HomeWeeklyRitualCard(
                ritual = ritual,
                onOpenReview = { onOpenRoute(AppRoute.Review) },
            )
        }
        uiState.updateNudge?.let { update ->
            HomeUpdateCard(
                update = update,
                onOpenProfile = onOpenProfile,
            )
        }
        HomeFocusCard(uiState = uiState, onOpenTasks = { onOpenRoute(AppRoute.Tasks) })
        HomeInsightsTeaser(uiState = uiState, onOpenInsights = { onOpenRoute(AppRoute.Insights) })
        HomeCalendarCard(uiState = uiState, onOpenCalendar = { onOpenRoute(AppRoute.Calendar) })
        HomeAssistantCard(uiState = uiState, onOpenAssistant = { onOpenRoute(AppRoute.Assistant) })
    }
}

@Composable
private fun HomeSyncCard(uiState: HomeUiState) {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Sync",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    uiState.syncFreshness?.let { freshness ->
                        Text(
                            text = freshness.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                SyncStatusPill(status = uiState.syncStatus)
            }
            uiState.syncFreshness?.supportingLabel?.let { supportingLabel ->
                Text(
                    text = supportingLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
private fun HomeQuickActionsRow(
    uiState: HomeUiState,
    onOpenRoute: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        uiState.quickActions.forEach { action ->
            AppCard(
                modifier = Modifier.width(172.dp),
                elevated = false,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = action.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = action.supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = when (action.label) {
                            "Tasks" -> ({ onOpenRoute(AppRoute.Tasks) })
                            "Finance" -> ({ onOpenRoute(AppRoute.Finance) })
                            "Calendar" -> ({ onOpenRoute(AppRoute.Calendar) })
                            "Assistant" -> ({ onOpenRoute(AppRoute.Assistant) })
                            else -> ({ onOpenRoute(AppRoute.Review) })
                        },
                    ) {
                        Text("Open")
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeWeeklyRitualCard(
    ritual: HomeWeeklyRitualUiModel,
    onOpenReview: () -> Unit,
) {
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
            Button(onClick = onOpenReview) {
                Text(ritual.ctaLabel)
            }
        }
    }
}

@Composable
private fun HomeUpdateCard(
    update: com.personal.lifeOS.core.ui.model.UpdateNudgeUiModel,
    onOpenProfile: () -> Unit,
) {
    AppCard(elevated = false) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = update.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = update.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpenProfile) {
                Text(if (update.required) "Install update" else "Review update")
            }
        }
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
                    )
                    Text(
                        text = "Insights",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text =
                        uiState.topInsight
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
