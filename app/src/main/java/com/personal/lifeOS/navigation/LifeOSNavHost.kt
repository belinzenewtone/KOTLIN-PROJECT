@file:Suppress("MaxLineLength")

package com.personal.lifeOS.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.personal.lifeOS.core.permissions.AppPermissionsOrchestrator
import com.personal.lifeOS.core.security.BiometricAuthManager
import com.personal.lifeOS.core.ui.designsystem.AppDesignTokens
import com.personal.lifeOS.core.update.presentation.OtaUpdatePromptHost
import com.personal.lifeOS.feature.assistant.presentation.AssistantScreen
import com.personal.lifeOS.feature.auth.presentation.AuthScreen
import com.personal.lifeOS.feature.auth.presentation.OnboardingScreen
import com.personal.lifeOS.feature.budget.presentation.BudgetScreen
import com.personal.lifeOS.feature.calendar.presentation.CalendarScreen
import com.personal.lifeOS.feature.export.presentation.ExportScreen
import com.personal.lifeOS.feature.finance.presentation.FinanceScreen
import com.personal.lifeOS.feature.home.presentation.HomeScreen
import com.personal.lifeOS.feature.income.presentation.IncomeScreen
import com.personal.lifeOS.feature.profile.presentation.ProfileScreen
import com.personal.lifeOS.feature.recurring.presentation.RecurringScreen
import com.personal.lifeOS.feature.search.presentation.SearchScreen
import com.personal.lifeOS.feature.settings.presentation.SettingsScreen
import com.personal.lifeOS.feature.tasks.presentation.TasksScreen
import com.personal.lifeOS.features.auth.presentation.AuthUiEvent
import com.personal.lifeOS.features.auth.presentation.AuthViewModel
import com.personal.lifeOS.features.insights.presentation.InsightsScreen
import com.personal.lifeOS.features.learning.presentation.LearningScreen
import com.personal.lifeOS.features.planner.presentation.PlannerScreen
import com.personal.lifeOS.features.review.presentation.ReviewScreen

// Re-prompt biometric only if app was backgrounded for longer than this threshold.
private const val BIOMETRIC_LOCK_TIMEOUT_MS = 5 * 60 * 1000L

private data class BiometricLockState(
    val requiresLock: Boolean,
    val appContentUnlocked: Boolean,
    val errorMessage: String?,
    val onRetry: () -> Unit,
)

@Composable
@Suppress("CyclomaticComplexMethod")
@OptIn(ExperimentalLayoutApi::class)
fun LifeOSNavHost(
    biometricEnabled: Boolean,
    startDestination: String = AppRoute.Auth,
    shouldCheckForUpdates: Boolean = true,
    onSensitiveScreenChanged: (Boolean) -> Unit = {},
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    val currentRoute = currentDestination?.route
    val isOnPublicFlow = isPublicRoute(currentRoute)
    val requiresBiometricLock = authState.isLoggedIn && biometricEnabled && !isOnPublicFlow
    val lockState = rememberBiometricLockState(requiresBiometricLock)

    LaunchedEffect(authState.isLoggedIn, authState.onboardingCompleted, currentRoute) {
        when (
            val guardRoute =
                resolveGuardNavigationTarget(
                    isLoggedIn = authState.isLoggedIn,
                    onboardingCompleted = authState.onboardingCompleted,
                    currentRoute = currentRoute,
                )
        ) {
            null -> Unit
            else ->
                if (guardRoute == AppRoute.Auth) {
                    navController.navigateToAuth()
                } else {
                    navController.navigate(guardRoute) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
        }
    }

    LaunchedEffect(currentRoute, authState.isLoggedIn) {
        onSensitiveScreenChanged(!isOnPublicFlow && isSensitiveRoute(currentRoute))
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        LifeOSNavigationGraph(
            navController = navController,
            authViewModel = authViewModel,
            startDestination = startDestination,
        )

        // Hide the floating nav bar when the keyboard is visible — it otherwise
        // overlaps the input bar and wastes vertical space while the user is typing.
        val isImeVisible = WindowInsets.isImeVisible
        val showBottomBar =
            currentRoute != null &&
                !isOnPublicFlow &&
                lockState.appContentUnlocked &&
                !isImeVisible
        AnimatedVisibility(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            visible = showBottomBar,
            enter = fadeIn(tween(220, easing = EaseInOut)),
            exit = fadeOut(tween(160, easing = EaseInOut)),
        ) {
            LifeOSBottomBar(
                navController = navController,
                currentDestination = currentDestination,
            )
        }
        val shouldShowOtaPrompt =
            shouldCheckForUpdates &&
                (
                    isOnPublicFlow ||
                        lockState.appContentUnlocked
                )
        if (shouldShowOtaPrompt) {
            OtaUpdatePromptHost(shouldCheckForUpdates = shouldCheckForUpdates)
        }

        // Contextual, one-time permission rationale cards — appear at natural
        // moments (Home → notifications, Finance → SMS/M-Pesa). Never nag.
        if (!isOnPublicFlow && lockState.appContentUnlocked) {
            AppPermissionsOrchestrator(currentRoute = currentRoute)
        }

        if (lockState.requiresLock) {
            BiometricLockOverlay(
                errorMessage = lockState.errorMessage,
                onRetry = lockState.onRetry,
                onSignOut = {
                    authViewModel.onEvent(AuthUiEvent.SignOut)
                    navController.navigateToAuth()
                },
            )
        }
    }
}

@Composable
private fun rememberBiometricLockState(requiresBiometricLock: Boolean): BiometricLockState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isBiometricUnlocked by rememberSaveable { mutableStateOf(false) }
    var shouldPromptBiometric by rememberSaveable { mutableStateOf(false) }
    var biometricError by rememberSaveable { mutableStateOf<String?>(null) }
    // Track when the app moved to background for session-timeout enforcement.
    var backgroundedAtMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(requiresBiometricLock) {
        isBiometricUnlocked = false
        shouldPromptBiometric = requiresBiometricLock
        biometricError = null
    }

    DisposableEffect(lifecycleOwner, requiresBiometricLock) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        backgroundedAtMs = System.currentTimeMillis()
                    }
                    Lifecycle.Event.ON_START -> {
                        if (requiresBiometricLock) {
                            val elapsed = System.currentTimeMillis() - backgroundedAtMs
                            // Only relock after the timeout; brief interruptions (permission
                            // dialogs, notification shade) won't force a re-prompt.
                            if (elapsed >= BIOMETRIC_LOCK_TIMEOUT_MS || !isBiometricUnlocked) {
                                isBiometricUnlocked = false
                                shouldPromptBiometric = true
                                biometricError = null
                            }
                        }
                    }
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(shouldPromptBiometric, requiresBiometricLock, isBiometricUnlocked) {
        if (!requiresBiometricLock || isBiometricUnlocked || !shouldPromptBiometric) return@LaunchedEffect

        val activity = context.findFragmentActivity()
        if (activity == null) {
            biometricError = "Biometric prompt unavailable in this context."
            shouldPromptBiometric = false
            return@LaunchedEffect
        }

        if (!BiometricAuthManager.canAuthenticate(activity)) {
            biometricError = "Biometric authentication is not available on this device."
            shouldPromptBiometric = false
            return@LaunchedEffect
        }

        shouldPromptBiometric = false
        BiometricAuthManager.authenticate(
            activity = activity,
            onSuccess = {
                isBiometricUnlocked = true
                biometricError = null
            },
            onError = { message ->
                biometricError = message
            },
        )
    }

    return BiometricLockState(
        requiresLock = requiresBiometricLock && !isBiometricUnlocked,
        appContentUnlocked = !requiresBiometricLock || isBiometricUnlocked,
        errorMessage = biometricError,
        onRetry = {
            biometricError = null
            shouldPromptBiometric = true
        },
    )
}

@Composable
@Suppress("LongMethod")
private fun LifeOSNavigationGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    startDestination: String,
) {
    NavHost(
        navController = navController,
        startDestination = canonicalRoute(startDestination),
        modifier = Modifier.fillMaxSize(),
        // Screen transitions — forward: fluid slide-in from right with spring; back: slide out.
        // Spring damping gives a natural, intentional feel rather than a mechanical tween.
        enterTransition = {
            fadeIn(tween(260, easing = EaseInOut)) +
                slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) { it / 4 }
        },
        exitTransition = {
            fadeOut(tween(200, easing = EaseInOut)) +
                slideOutHorizontally(tween(200, easing = EaseInOut)) { -it / 8 }
        },
        popEnterTransition = {
            fadeIn(tween(220, easing = EaseInOut)) +
                slideInHorizontally(tween(220, easing = EaseInOut)) { -it / 8 }
        },
        popExitTransition = {
            fadeOut(tween(240, easing = EaseInOut)) +
                slideOutHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) { it / 4 }
        },
    ) {
        composable(AppRoute.Auth) {
            AuthScreen(
                viewModel = authViewModel,
                onAuthenticated = {
                    if (authViewModel.uiState.value.onboardingCompleted) {
                        navController.navigate(AppRoute.Home) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(AppRoute.Onboarding) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
        composable(AppRoute.Onboarding) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(AppRoute.Home) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBackToAuth = {
                    navController.navigate(AppRoute.Home) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        // ── Primary tab screens ──────────────────────────────────────────────
        composable(AppRoute.Home) {
            HomeScreen(
                onOpenRoute = navController::navigate,
                onOpenProfile = { navController.navigate(AppRoute.Profile) },
            )
        }
        composable(AppRoute.LegacyDashboard) {
            HomeScreen(
                onOpenRoute = navController::navigate,
                onOpenProfile = { navController.navigate(AppRoute.Profile) },
            )
        }
        composable(AppRoute.Tasks) { TasksScreen() }
        composable(AppRoute.Finance) {
            FinanceScreen(
                onOpenTools = { navController.navigate(AppRoute.Planner) },
            )
        }
        composable(AppRoute.LegacyExpenses) {
            FinanceScreen(
                onOpenTools = { navController.navigate(AppRoute.Planner) },
            )
        }
        composable(AppRoute.Calendar) { CalendarScreen() }
        composable(AppRoute.Assistant) { AssistantScreen() }

        // ── Secondary screens ────────────────────────────────────────────────
        composable(AppRoute.Profile) {
            ProfileScreen(
                authViewModel = authViewModel,
                onSignOut = {
                    authViewModel.onEvent(AuthUiEvent.SignOut)
                    navController.navigateToAuth()
                },
            )
        }
        composable(AppRoute.Settings) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoute.Export) {
            ExportScreen(onBack = { navController.popBackStack() })
        }

        // Insights — accessible via Home header button (not a bottom tab per design decision)
        composable(AppRoute.Analytics) {
            InsightsScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoute.Insights) {
            InsightsScreen(onBack = { navController.popBackStack() })
        }

        composable(AppRoute.Search) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenResult = { result ->
                    result.navigationTarget?.let { target ->
                        navController.navigate(target) {
                            launchSingleTop = true
                        }
                    }
                },
            )
        }

        // Finance Tools hub — launched from Finance screen's top bar
        composable(AppRoute.Planner) {
            PlannerScreen(
                onBack = { navController.popBackStack() },
                onOpenBudget = { navController.navigate(AppRoute.Budget) },
                onOpenIncome = { navController.navigate(AppRoute.Income) },
                onOpenRecurring = { navController.navigate(AppRoute.Recurring) },
                onOpenExport = { navController.navigate(AppRoute.Export) },
                onOpenSearch = { navController.navigate(AppRoute.Search) },
            )
        }

        // Finance sub-screens
        composable(AppRoute.Budget) { BudgetScreen(onBack = { navController.popBackStack() }) }
        composable(AppRoute.Income) { IncomeScreen(onBack = { navController.popBackStack() }) }
        composable(AppRoute.Recurring) { RecurringScreen(onBack = { navController.popBackStack() }) }

        // Weekly/monthly personal digest
        composable(AppRoute.Review) { ReviewScreen(onBack = { navController.popBackStack() }) }

        // Learning module
        composable(AppRoute.Learning) { LearningScreen(onBack = { navController.popBackStack() }) }
    }
}

@Composable
private fun LifeOSBottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
) {
    val navBarShape = RoundedCornerShape(AppDesignTokens.radius.lg)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                // Narrower side margins so the bar stretches wider and feels balanced
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                // Sit just above the system nav keys / gesture bar — no extra gap
                .padding(bottom = 4.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = navBarShape,
                        ambientColor = Color.Black.copy(alpha = 0.4f),
                        spotColor = Color.Black.copy(alpha = 0.4f),
                    )
                    .clip(navBarShape)
                    // Solid surface base — legible in both light and dark mode
                    .background(MaterialTheme.colorScheme.surface)
                    // Subtle glass sheen on top
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        Color.White.copy(alpha = 0.10f),
                                        Color.White.copy(alpha = 0.04f),
                                    ),
                            ),
                    )
                    .border(
                        width = 1.dp,
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        Color.White.copy(alpha = 0.15f),
                                        Color.White.copy(alpha = 0.04f),
                                    ),
                            ),
                        shape = navBarShape,
                    )
                    // Reduced vertical padding → slimmer bar height
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val activeRoutes = currentDestination?.hierarchy?.mapNotNull { it.route }?.toSet().orEmpty()
            primaryTabs.forEach { item ->
                val selected = isPrimaryTabSelected(item, activeRoutes)
                BottomNavItem(
                    label = item.label,
                    selected = selected,
                    selectedIcon = item.selectedIcon,
                    unselectedIcon = item.unselectedIcon,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? {
    return when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.findFragmentActivity()
        else -> null
    }
}

@Composable
private fun RowScope.BottomNavItem(
    label: String,
    selected: Boolean,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 220, easing = EaseInOut),
        label = "iconColor",
    )

    Column(
        modifier =
            Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Animated pill indicator — scales in/out smoothly
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(180)),
                exit  = scaleOut(tween(140)) + fadeOut(tween(140)),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                )
            }
            // Animated icon crossfade between selected and unselected variants
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                },
                label = "tabIcon",
            ) { isSelected ->
                Icon(
                    imageVector = if (isSelected) selectedIcon else unselectedIcon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = label,
            color = iconColor,
            fontSize = 11.sp,
            maxLines = 1,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}

@Composable
private fun BiometricLockOverlay(
    errorMessage: String?,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
) {
    val lockCardShape = RoundedCornerShape(AppDesignTokens.radius.lg)
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(32.dp)
                    .clip(lockCardShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = lockCardShape,
                    )
                    .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Fingerprint icon
            Icon(
                imageVector = Icons.Outlined.Fingerprint,
                contentDescription = null,
                tint = if (errorMessage != null)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp),
            )
            Text(
                text = if (errorMessage != null) "Unlock failed" else "Unlock LifeOS",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = errorMessage
                    ?: "Touch the fingerprint sensor or use your device PIN to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onSignOut) {
                    Text("Sign Out")
                }
                Button(onClick = onRetry) {
                    Text(if (errorMessage != null) "Try Again" else "Unlock")
                }
            }
        }
    }
}

private fun NavHostController.navigateToAuth() {
    navigate(AppRoute.Auth) {
        popUpTo(0) { inclusive = true }
    }
}
