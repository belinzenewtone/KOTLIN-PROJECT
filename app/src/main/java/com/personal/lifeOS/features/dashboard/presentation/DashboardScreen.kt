package com.personal.lifeOS.features.dashboard.presentation

import androidx.compose.runtime.Composable
import com.personal.lifeOS.feature.home.presentation.HomeScreen

/**
 * Compatibility bridge retained during phased package migration.
 * Legacy dashboard entrypoint now renders the unified home screen.
 */
@Composable
fun DashboardScreen(
    onOpenTasks: () -> Unit = {},
    onOpenFinance: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onOpenAssistant: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    onOpenLearning: () -> Unit = {},
) {
    HomeScreen(
        onOpenTasks = onOpenTasks,
        onOpenFinance = onOpenFinance,
        onOpenCalendar = onOpenCalendar,
        onOpenAssistant = onOpenAssistant,
        onOpenProfile = onOpenProfile,
        onOpenInsights = onOpenInsights,
        onOpenLearning = onOpenLearning,
    )
}
