package com.personal.lifeOS.feature.planner.presentation

import androidx.compose.runtime.Composable
import com.personal.lifeOS.features.planner.presentation.PlannerScreen as LegacyPlannerScreen

/**
 * Feature-package bridge retained during phased package migration.
 */
@Composable
fun PlannerScreen(
    onBack: (() -> Unit)? = null,
    onOpenBudget: () -> Unit = {},
    onOpenIncome: () -> Unit = {},
    onOpenRecurring: () -> Unit = {},
    onOpenExport: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
) {
    LegacyPlannerScreen(
        onBack = onBack,
        onOpenBudget = onOpenBudget,
        onOpenIncome = onOpenIncome,
        onOpenRecurring = onOpenRecurring,
        onOpenExport = onOpenExport,
        onOpenSearch = onOpenSearch,
    )
}
