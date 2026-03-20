package com.personal.lifeOS.navigation

internal fun isPublicRoute(route: String?): Boolean {
    if (route == null) return true
    return route == AppRoute.Auth || route == AppRoute.Onboarding
}

internal fun canonicalRoute(route: String): String {
    return when (route) {
        AppRoute.LegacyDashboard -> AppRoute.Home
        AppRoute.LegacyExpenses -> AppRoute.Finance
        else -> route
    }
}

internal fun resolveGuardNavigationTarget(
    isLoggedIn: Boolean,
    onboardingCompleted: Boolean,
    currentRoute: String?,
): String? {
    val shouldRouteToOnboarding = isLoggedIn && !onboardingCompleted
    return when {
        !isLoggedIn && currentRoute != AppRoute.Auth ->
            AppRoute.Auth

        shouldRouteToOnboarding && currentRoute != AppRoute.Onboarding ->
            AppRoute.Onboarding

        isLoggedIn &&
            !shouldRouteToOnboarding &&
            (currentRoute == AppRoute.Auth || currentRoute == AppRoute.Onboarding) ->
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
