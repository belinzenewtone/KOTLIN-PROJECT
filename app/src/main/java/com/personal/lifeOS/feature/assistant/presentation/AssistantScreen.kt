package com.personal.lifeOS.feature.assistant.presentation

import androidx.compose.runtime.Composable
import com.personal.lifeOS.features.assistant.presentation.AssistantScreen as LegacyAssistantScreen

/**
 * Feature-package bridge retained during phased package migration.
 */
@Composable
fun AssistantScreen() {
    LegacyAssistantScreen()
}
