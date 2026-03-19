package com.personal.lifeOS.feature.auth.presentation

import androidx.compose.runtime.Composable
import com.personal.lifeOS.features.auth.presentation.OnboardingScreen as LegacyOnboardingScreen

/**
 * Feature-package bridge retained during phased package migration.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onBackToAuth: () -> Unit,
) {
    LegacyOnboardingScreen(
        onFinished = onFinished,
        onBackToAuth = onBackToAuth,
    )
}
