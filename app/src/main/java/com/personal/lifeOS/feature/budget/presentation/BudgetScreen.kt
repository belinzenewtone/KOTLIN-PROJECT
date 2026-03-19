package com.personal.lifeOS.feature.budget.presentation

import androidx.compose.runtime.Composable
import com.personal.lifeOS.features.budget.presentation.BudgetScreen as LegacyBudgetScreen

/**
 * Feature-package bridge retained during phased package migration.
 */
@Composable
fun BudgetScreen() {
    LegacyBudgetScreen()
}
