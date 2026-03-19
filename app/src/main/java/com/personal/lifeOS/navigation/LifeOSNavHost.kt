@file:Suppress("MaxLineLength")

package com.personal.lifeOS.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.personal.lifeOS.feature.analytics.presentation.AnalyticsScreen
import com.personal.lifeOS.feature.assistant.presentation.AssistantScreen
import com.personal.lifeOS.feature.auth.presentation.AuthScreen
import com.personal.lifeOS.feature.auth.presentation.OnboardingScreen
import com.personal.lifeOS.feature.calendar.presentation.CalendarScreen
import com.personal.lifeOS.feature.export.presentation.ExportScreen
import com.personal.lifeOS.feature.finance.presentation.FinanceScreen
import com.personal.lifeOS.feature.home.presentation.HomeScreen
import com.personal.lifeOS.feature.planner.presentation.PlannerScreen
import com.personal.lifeOS.feature.profile.presentation.ProfileScreen
import com.personal.lifeOS.feature.review.presentation.ReviewScreen
import com.personal.lifeOS.feature.search.presentation.SearchScreen
import com.personal.lifeOS.feature.settings.presentation.SettingsScreen
import com.personal.lifeOS.feature.tasks.presentation.TasksScreen
import com.personal.lifeOS.core.security.BiometricAuthManager
import com.personal.lifeOS.core.update.presentation.OtaUpdatePromptHost
import com.personal.lifeOS.features.auth.presentation.AuthUiEvent
import com.personal.lifeOS.features.auth.presentation.AuthViewModel
import com.personal.lifeOS.ui.theme.BackgroundDark
import com.personal.lifeOS.ui.theme.Primary
import com.personal.lifeOS.ui.theme.TextPrimary
import com.personal.lifeOS.ui.theme.TextTertiary

private data class BiometricLockState(
    val requiresLock: Boolean,
    val appContentUnlocked: Boolean,
    val errorMessage: String?,
    val onRetry: () -> Unit,
)

@Composable
@Suppress("CyclomaticComplexMethod")
fun LifeOSNavHost(
    biometricEnabled: Boolean,
    startDestination: String = AppRoute.Auth,
    shouldCheckForUpdates: Boolean = true,
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

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackgroundDark),
    ) {
        LifeOSNavigationGraph(
            navController = navController,
            authViewModel = authViewModel,
            startDestination = startDestination,
        )

        if (!isOnPublicFlow && authState.isLoggedIn && lockState.appContentUnlocked) {
            LifeOSBottomBar(
                navController = navController,
                currentDestination = currentDestination,
            )
            OtaUpdatePromptHost(shouldCheckForUpdates = shouldCheckForUpdates)
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

    LaunchedEffect(requiresBiometricLock) {
        isBiometricUnlocked = false
        shouldPromptBiometric = requiresBiometricLock
        biometricError = null
    }

    DisposableEffect(lifecycleOwner, requiresBiometricLock) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START && requiresBiometricLock) {
                    isBiometricUnlocked = false
                    shouldPromptBiometric = true
                    biometricError = null
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(shouldPromptBiometric, requiresBiometricLock, isBiometricUnlocked) {
        if (!requiresBiometricLock || isBiometricUnlocked || !shouldPromptBiometric) return@LaunchedEffect

        val activity = context as? FragmentActivity
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
private fun LifeOSNavigationGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    startDestination: String,
) {
    NavHost(
        navController = navController,
        startDestination = canonicalRoute(startDestination),
        modifier = Modifier.fillMaxSize(),
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
                    authViewModel.onEvent(AuthUiEvent.SignOut)
                    navController.navigateToAuth()
                },
            )
        }
        composable(AppRoute.Home) {
            HomeScreen(
                onOpenTasks = { navController.navigate(AppRoute.Tasks) },
                onOpenFinance = { navController.navigate(AppRoute.Finance) },
                onOpenCalendar = { navController.navigate(AppRoute.Calendar) },
                onOpenAssistant = { navController.navigate(AppRoute.Assistant) },
                onOpenProfile = { navController.navigate(AppRoute.Profile) },
            )
        }
        composable(AppRoute.LegacyDashboard) {
            HomeScreen(
                onOpenTasks = { navController.navigate(AppRoute.Tasks) },
                onOpenFinance = { navController.navigate(AppRoute.Finance) },
                onOpenCalendar = { navController.navigate(AppRoute.Calendar) },
                onOpenAssistant = { navController.navigate(AppRoute.Assistant) },
                onOpenProfile = { navController.navigate(AppRoute.Profile) },
            )
        }
        composable(AppRoute.Tasks) { TasksScreen() }
        composable(AppRoute.Finance) { FinanceScreen() }
        composable(AppRoute.LegacyExpenses) { FinanceScreen() }
        composable(AppRoute.Calendar) { CalendarScreen() }
        composable(AppRoute.Assistant) { AssistantScreen() }

        composable(AppRoute.Profile) {
            ProfileScreen(
                authViewModel = authViewModel,
                onSignOut = {
                    authViewModel.onEvent(AuthUiEvent.SignOut)
                    navController.navigateToAuth()
                },
            )
        }
        composable(AppRoute.Settings) { SettingsScreen() }
        composable(AppRoute.Export) { ExportScreen() }
        composable(AppRoute.Analytics) { AnalyticsScreen() }
        composable(AppRoute.Search) { SearchScreen() }
        composable(AppRoute.Planner) { PlannerScreen() }
        composable(AppRoute.Review) { ReviewScreen() }
    }
}

@Composable
private fun BoxScope.LifeOSBottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
) {
    Box(
        modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(alpha = 0.4f),
                        spotColor = Color.Black.copy(alpha = 0.4f),
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        Color.White.copy(alpha = 0.12f),
                                        Color.White.copy(alpha = 0.06f),
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
                        shape = RoundedCornerShape(24.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp),
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

@Composable
private fun RowScope.BottomNavItem(
    label: String,
    selected: Boolean,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val iconColor by animateColorAsState(
        targetValue = if (selected) Primary else TextTertiary,
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
                .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (selected) {
                Box(
                    modifier =
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.15f)),
                )
            }
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = label,
            color = iconColor,
            fontSize = 9.sp,
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
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BackgroundDark)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Biometric Unlock Required",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text = errorMessage ?: "Authenticate with fingerprint to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRetry) {
                    Text("Retry")
                }
                OutlinedButton(onClick = onSignOut) {
                    Text("Sign Out")
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
