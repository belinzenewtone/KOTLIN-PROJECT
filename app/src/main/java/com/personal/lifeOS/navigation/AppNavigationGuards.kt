package com.personal.lifeOS.navigation

internal fun isPublicRoute(route: String?): Boolean {
    if (route == null) return false
    return route == AppRoute.Auth || route == AppRoute.Onboarding
}

internal fun resolveGuardNavigationTarget(
    isLoggedIn: Boolean,
    onboardingCompleted: Boolean,
    currentRoute: String?,
): String? {
    val shouldRouteToOnboarding = !onboardingCompleted
    return when {
        shouldRouteToOnboarding &&
            currentRoute != AppRoute.Onboarding &&
            currentRoute != AppRoute.Auth ->
            AppRoute.Onboarding

        !shouldRouteToOnboarding && currentRoute == AppRoute.Onboarding ->
            AppRoute.Home

        isLoggedIn && currentRoute == AppRoute.Auth ->
            AppRoute.Home

        else -> null
    }
}

internal fun isPrimaryTabSelected(
    tab: AppPrimaryTab,
    activeRoutes: Set<String>,
): Boolean {
    return tab.routeAliases.any(activeRoutes::contains)
}

internal fun isSensitiveRoute(route: String?): Boolean {
    if (route == null) return false
    return route in
        setOf(
            AppRoute.Finance,
            AppRoute.Planner,
            AppRoute.Budget,
            AppRoute.Income,
            AppRoute.Recurring,
            AppRoute.Loans,
            AppRoute.Categorize,
            AppRoute.FeeAnalytics,
            AppRoute.Review,
            AppRoute.Export,
            AppRoute.Search,
            AppRoute.Profile,
            AppRoute.Assistant,
        ) || route?.startsWith(AppRoute.MerchantDetail) == true
}
