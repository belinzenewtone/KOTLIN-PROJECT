package com.personal.lifeOS.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.personal.lifeOS.features.assistant.presentation.AssistantScreen
import com.personal.lifeOS.features.calendar.presentation.CalendarScreen
import com.personal.lifeOS.features.dashboard.presentation.DashboardScreen
import com.personal.lifeOS.features.expenses.presentation.ExpensesScreen
import com.personal.lifeOS.features.tasks.presentation.TasksScreen
import com.personal.lifeOS.ui.theme.BackgroundDark
import com.personal.lifeOS.ui.theme.GlassWhite
import com.personal.lifeOS.ui.theme.Primary
import com.personal.lifeOS.ui.theme.TextSecondary
import com.personal.lifeOS.ui.theme.TextTertiary

@Composable
fun LifeOSNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0D1117),
                contentColor = TextSecondary,
                tonalElevation = androidx.compose.ui.unit.dp.times(0)
            ) {
                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == item.screen.route
                    } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            unselectedIconColor = TextTertiary,
                            unselectedTextColor = TextTertiary,
                            indicatorColor = Primary.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Calendar.route) { CalendarScreen() }
            composable(Screen.Expenses.route) { ExpensesScreen() }
            composable(Screen.Tasks.route) { TasksScreen() }
            composable(Screen.Assistant.route) { AssistantScreen() }
        }
    }
}
