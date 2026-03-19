package com.personal.lifeOS.feature.calendar.presentation

import androidx.compose.runtime.Composable
import com.personal.lifeOS.features.calendar.presentation.CalendarScreen as LegacyCalendarScreen

/**
 * Feature-package bridge retained during phased package migration.
 */
@Composable
fun CalendarScreen() {
    LegacyCalendarScreen()
}
