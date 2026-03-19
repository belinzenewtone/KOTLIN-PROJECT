package com.personal.lifeOS.feature.auth.presentation

import androidx.compose.runtime.Composable
import com.personal.lifeOS.features.auth.presentation.AuthScreen as LegacyAuthScreen
import com.personal.lifeOS.features.auth.presentation.AuthViewModel

/**
 * Feature-package bridge retained during phased package migration.
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit,
) {
    LegacyAuthScreen(
        viewModel = viewModel,
        onAuthenticated = onAuthenticated,
    )
}
