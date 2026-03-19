package com.personal.lifeOS.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationGuardsTest {
    @Test
    fun `logged out users are routed to auth when on protected routes`() {
        val target =
            resolveGuardNavigationTarget(
                isLoggedIn = false,
                onboardingCompleted = false,
                currentRoute = AppRoute.Home,
            )

        assertEquals(AppRoute.Auth, target)
    }

    @Test
    fun `logged out users stay on auth route`() {
        val target =
            resolveGuardNavigationTarget(
                isLoggedIn = false,
                onboardingCompleted = false,
                currentRoute = AppRoute.Auth,
            )

        assertNull(target)
    }

    @Test
    fun `logged in users without onboarding completion are routed to onboarding`() {
        val target =
            resolveGuardNavigationTarget(
                isLoggedIn = true,
                onboardingCompleted = false,
                currentRoute = AppRoute.Home,
            )

        assertEquals(AppRoute.Onboarding, target)
    }

    @Test
    fun `fully onboarded users are routed to home from auth surfaces`() {
        val fromAuth =
            resolveGuardNavigationTarget(
                isLoggedIn = true,
                onboardingCompleted = true,
                currentRoute = AppRoute.Auth,
            )
        val fromOnboarding =
            resolveGuardNavigationTarget(
                isLoggedIn = true,
                onboardingCompleted = true,
                currentRoute = AppRoute.Onboarding,
            )

        assertEquals(AppRoute.Home, fromAuth)
        assertEquals(AppRoute.Home, fromOnboarding)
    }

    @Test
    fun `fully onboarded users stay on protected routes`() {
        val target =
            resolveGuardNavigationTarget(
                isLoggedIn = true,
                onboardingCompleted = true,
                currentRoute = AppRoute.Tasks,
            )

        assertNull(target)
    }

    @Test
    fun `public route detection only returns true for auth and onboarding`() {
        assertTrue(isPublicRoute(AppRoute.Auth))
        assertTrue(isPublicRoute(AppRoute.Onboarding))
        assertFalse(isPublicRoute(AppRoute.Home))
        assertFalse(isPublicRoute(null))
    }

    @Test
    fun `canonical route normalizes legacy aliases to primary tab routes`() {
        assertEquals(AppRoute.Home, canonicalRoute(AppRoute.LegacyDashboard))
        assertEquals(AppRoute.Finance, canonicalRoute(AppRoute.LegacyExpenses))
        assertEquals(AppRoute.Tasks, canonicalRoute(AppRoute.Tasks))
    }

    @Test
    fun `primary tab selection supports legacy aliases`() {
        val home = primaryTabs.first { it.route == AppRoute.Home }
        val finance = primaryTabs.first { it.route == AppRoute.Finance }

        assertTrue(isPrimaryTabSelected(home, setOf(AppRoute.LegacyDashboard)))
        assertTrue(isPrimaryTabSelected(finance, setOf(AppRoute.LegacyExpenses)))
        assertFalse(isPrimaryTabSelected(finance, setOf(AppRoute.Home)))
    }
}
