package com.personal.lifeOS.feature.home.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.FinanceSummaryCard
import com.personal.lifeOS.core.ui.designsystem.LoadingState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.core.ui.designsystem.TopBanner
import com.personal.lifeOS.core.ui.designsystem.TopBannerTone
import com.personal.lifeOS.features.dashboard.presentation.DashboardViewModel
import com.personal.lifeOS.navigation.AppRoute
import com.personal.lifeOS.ui.theme.AppSpacing

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
        contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFloatingNav),
        topBanner = {
            uiState.errorMessage?.let {
                TopBanner(
                    message = it,
                    tone = TopBannerTone.ERROR,
                )
            }
        },
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

        Text(
            text = uiState.greeting,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        HomeSummaryStrip(uiState = uiState)
        HomeAgendaCard(uiState = uiState, onOpenRoute = onOpenRoute)
        uiState.weeklyRitual?.let { ritual ->
            HomeWeeklyRitualCard(
                ritual = ritual,
                onOpenReview = { onOpenRoute(AppRoute.Review) },
            )
        }
    }
}

@Composable
private fun HomeSummaryStrip(uiState: HomeUiState) {
    val metrics =
        listOf(
            "Today" to uiState.todaySpending,
            "Week" to uiState.weekSpending,
            "Month" to uiState.monthSpending,
        )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(metrics) { (title, amount) ->
            FinanceSummaryCard(
                title = title,
                amount = amount,
                modifier = Modifier.width(176.dp),
            )
        }
    }
}

@Composable
private fun HomeAgendaCard(
    uiState: HomeUiState,
    onOpenRoute: (String) -> Unit,
) {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            HomeAgendaRow(
                icon = Icons.Filled.TaskAlt,
                title = "Tasks",
                value =
                    if (uiState.pendingTaskCount == 0) {
                        "All done"
                    } else {
                        "${uiState.pendingTaskCount} pending"
                    },
                onClick = { onOpenRoute(AppRoute.Tasks) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            HomeAgendaRow(
                icon = Icons.Filled.CalendarMonth,
                title = "Next Event",
                value = uiState.nextEventTimeLabel ?: "No event",
                onClick = { onOpenRoute(AppRoute.Calendar) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            HomeAgendaRow(
                icon = Icons.Filled.AutoGraph,
                title = "Insights",
                value = "Trends",
                onClick = { onOpenRoute(AppRoute.Insights) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            HomeAgendaRow(
                icon = Icons.Filled.Search,
                title = "Search",
                value = "Explore",
                onClick = { onOpenRoute(AppRoute.Search) },
            )
        }
    }
}

@Composable
private fun HomeAgendaRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp),
            )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = ritual.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onOpenReview) {
                    Text(ritual.ctaLabel)
                }
            }
            Text(
                text = ritual.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}
