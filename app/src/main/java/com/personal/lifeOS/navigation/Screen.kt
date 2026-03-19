package com.personal.lifeOS.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")

    data object Calendar : Screen("calendar")

    data object Expenses : Screen("expenses")

    data object Tasks : Screen("tasks")

    data object Planner : Screen("planner")

    data object Assistant : Screen("assistant")

    data object Analytics : Screen("analytics")

    data object Export : Screen("export")

    data object Profile : Screen("profile")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomNavItems =
    listOf(
        BottomNavItem(Screen.Dashboard, "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        BottomNavItem(Screen.Calendar, "Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        BottomNavItem(Screen.Expenses, "Expenses", Icons.Filled.Receipt, Icons.Outlined.Receipt),
        BottomNavItem(Screen.Tasks, "Tasks", Icons.Filled.TaskAlt, Icons.Outlined.TaskAlt),
        BottomNavItem(Screen.Planner, "Plan", Icons.Filled.Analytics, Icons.Outlined.Analytics),
        BottomNavItem(Screen.Assistant, "AI", Icons.Filled.SmartToy, Icons.Outlined.SmartToy),
        BottomNavItem(Screen.Profile, "Profile", Icons.Filled.Person, Icons.Outlined.Person),
    )
