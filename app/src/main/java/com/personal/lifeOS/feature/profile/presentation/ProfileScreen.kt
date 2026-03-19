package com.personal.lifeOS.feature.profile.presentation

import androidx.compose.runtime.Composable
import com.personal.lifeOS.features.auth.presentation.AuthViewModel
import com.personal.lifeOS.features.profile.presentation.ProfileScreen as LegacyProfileScreen

/**
 * Feature-package bridge retained during phased package migration.
 */
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel? = null,
    onSignOut: (() -> Unit)? = null,
) {
    LegacyProfileScreen(
        authViewModel = authViewModel,
        onSignOut = onSignOut,
    )
}
