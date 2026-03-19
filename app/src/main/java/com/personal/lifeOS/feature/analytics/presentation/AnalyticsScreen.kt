package com.personal.lifeOS.feature.analytics.presentation

import androidx.compose.runtime.Composable
import com.personal.lifeOS.features.analytics.presentation.AnalyticsScreen as LegacyAnalyticsScreen

/**
 * Feature-package bridge retained during phased package migration.
 */
@Composable
fun AnalyticsScreen() {
    LegacyAnalyticsScreen()
}
