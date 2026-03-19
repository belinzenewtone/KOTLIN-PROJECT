package com.personal.lifeOS.features.expenses.presentation

import androidx.compose.runtime.Composable
import com.personal.lifeOS.feature.finance.presentation.FinanceScreen

/**
 * Compatibility bridge retained during phased package migration.
 * Legacy expenses entrypoint now renders the unified finance screen.
 */
@Composable
fun ExpensesScreen() {
    FinanceScreen()
}
