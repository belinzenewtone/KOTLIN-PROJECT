package com.personal.lifeOS.feature.recurring.presentation

import androidx.compose.runtime.Composable
import com.personal.lifeOS.features.recurring.presentation.RecurringScreen as LegacyRecurringScreen

/**
 * Feature-package bridge retained during phased package migration.
 */
@Composable
fun RecurringScreen() {
    LegacyRecurringScreen()
}
