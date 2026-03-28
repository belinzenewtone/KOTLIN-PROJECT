package com.personal.lifeOS.features.dashboard.presentation

import androidx.compose.runtime.Composable
import com.personal.lifeOS.feature.home.presentation.HomeScreen
import com.personal.lifeOS.navigation.AppRoute

/**
 * Compatibility bridge retained during phased package migration.
 * Legacy dashboard entrypoint now renders the unified home screen.
 */
@Composable
fun DashboardScreen(
    onOpenRoute: (String) -> Unit = {},
    onOpenProfile: () -> Unit = {},
) {
    HomeScreen(
        onOpenRoute = onOpenRoute,
        onOpenProfile = onOpenProfile,
    )
}
