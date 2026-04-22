package com.personal.lifeOS.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector

object AppRoute {
    const val Auth = "auth"
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val Tasks = "tasks"
    const val Finance = "finance"
    const val Calendar = "calendar"
    const val Assistant = "assistant"

    const val Profile = "profile"
    const val Settings = "settings"
    const val Export = "export"
    const val Insights = "insights"   // Insights surface — accessible via Home shortcuts
    const val Search = "search"
    const val Planner = "planner"     // Finance Tools hub (budget, income, recurring, export)
    const val Review = "review"       // Weekly/monthly personal digest
    const val Events = "events"       // Dedicated events list — opened from Home shortcut

    // Finance sub-screens — navigated from Finance Tools hub
    const val Budget = "budget"
    const val Income = "income"
    const val Recurring = "recurring"
    const val Loans = "loans"

    // Finance utility screens — navigated from Finance main screen
    const val Categorize = "categorize"
    const val FeeAnalytics = "fee_analytics"
    const val MerchantDetail = "merchant_detail"

    // Learning module
    const val Learning = "learning"

    // ── Deep-link helpers for search result navigation ─────────────────────
    // Build parameterised routes that open a specific item directly.
    fun tasksWithItem(taskId: Long) = "$Tasks?itemId=$taskId"
    fun calendarWithEvent(eventId: Long, eventDate: Long) =
        "$Calendar?eventId=$eventId&eventDate=$eventDate"

    // ── Merchant detail route with encoded merchant name ───────────────────
    fun merchantDetail(merchant: String) =
        "$MerchantDetail/${java.net.URLEncoder.encode(merchant, "UTF-8")}"
}

data class AppPrimaryTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val routeAliases: Set<String> = emptySet(),
)

val primaryTabs =
    listOf(
        AppPrimaryTab(
            route = AppRoute.Home,
            label = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            routeAliases = setOf(AppRoute.Home),
        ),
        AppPrimaryTab(
            route = AppRoute.Finance,
            label = "Finance",
            selectedIcon = Icons.Filled.Payments,
            unselectedIcon = Icons.Outlined.Payments,
            routeAliases = setOf(AppRoute.Finance),
        ),
        AppPrimaryTab(
            route = AppRoute.Calendar,
            label = "Calendar",
            selectedIcon = Icons.Filled.CalendarMonth,
            unselectedIcon = Icons.Outlined.CalendarMonth,
            routeAliases = setOf(AppRoute.Calendar),
        ),
        AppPrimaryTab(
            route = AppRoute.Assistant,
            label = "AI",
            selectedIcon = Icons.Filled.SmartToy,
            unselectedIcon = Icons.Outlined.SmartToy,
            routeAliases = setOf(AppRoute.Assistant),
        ),
        AppPrimaryTab(
            route = AppRoute.Profile,
            label = "Profile",
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person,
            routeAliases = setOf(AppRoute.Profile),
        ),
    )
