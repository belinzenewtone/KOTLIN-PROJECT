package com.personal.lifeOS.feature.planner.presentation

import androidx.compose.runtime.Composable
import com.personal.lifeOS.features.planner.presentation.PlannerScreen as LegacyPlannerScreen

/**
 * Feature-package bridge retained during phased package migration.
 */
@Composable
fun PlannerScreen() {
    LegacyPlannerScreen()
}
