package com.personal.lifeOS.feature.tasks.presentation

import androidx.compose.runtime.Composable
import com.personal.lifeOS.features.tasks.presentation.TasksScreen as LegacyTasksScreen

/**
 * Feature-package bridge retained during phased package migration.
 */
@Composable
fun TasksScreen() {
    LegacyTasksScreen()
}
