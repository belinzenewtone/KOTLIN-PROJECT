package com.personal.lifeOS.features.auth.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.core.ui.designsystem.AppDesignTokens
import com.personal.lifeOS.ui.components.StyledSnackbarHost

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) onAuthenticated()
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(AuthUiEvent.ClearError)
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(AuthUiEvent.ClearSuccess)
        }
    }

    if (state.isLoading && !state.isLoggedIn) {
        AuthLoadingState()
        return
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = AppDesignTokens.spacing.lg)
                    .padding(top = AppDesignTokens.spacing.lg, bottom = AppDesignTokens.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(AppDesignTokens.spacing.md),
        ) {
            AuthBrandingHeader(isSignUpMode = state.isSignUpMode)
            Spacer(Modifier.height(4.dp))

            AnimatedVisibility(
                visible = !state.isSignUpMode,
                enter = fadeIn(tween(300, easing = EaseInOut)) +
                    expandVertically(tween(320, easing = EaseInOut)),
                exit = fadeOut(tween(200, easing = EaseInOut)) +
                    shrinkVertically(tween(220, easing = EaseInOut)),
            ) {
                SignInCard(state = state, viewModel = viewModel)
            }

            AnimatedVisibility(
                visible = state.isSignUpMode,
                enter = fadeIn(tween(300, easing = EaseInOut)) +
                    expandVertically(tween(320, easing = EaseInOut)),
                exit = fadeOut(tween(200, easing = EaseInOut)) +
                    shrinkVertically(tween(220, easing = EaseInOut)),
            ) {
                SignUpCard(state = state, viewModel = viewModel)
            }
        }

        StyledSnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
        )
    }
}
