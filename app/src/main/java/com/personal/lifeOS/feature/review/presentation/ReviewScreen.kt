package com.personal.lifeOS.feature.review.presentation

import androidx.compose.runtime.Composable
import com.personal.lifeOS.feature.planner.presentation.PlannerScreen

/**
 * Compatibility bridge retained during phased package migration.
 * Review currently reuses the planner workspace surface.
 */
@Composable
fun ReviewScreen() {
    PlannerScreen()
}
