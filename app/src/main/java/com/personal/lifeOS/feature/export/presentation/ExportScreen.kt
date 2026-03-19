package com.personal.lifeOS.feature.export.presentation

import androidx.compose.runtime.Composable
import com.personal.lifeOS.features.export.presentation.ExportScreen as LegacyExportScreen

/**
 * Feature-package bridge retained during phased package migration.
 */
@Composable
fun ExportScreen() {
    LegacyExportScreen()
}
