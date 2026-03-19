package com.personal.lifeOS.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationContractTest {
    @Test
    fun `primary tabs remain five-tab shell`() {
        assertEquals(5, primaryTabs.size)
        assertEquals(AppRoute.Home, primaryTabs[0].route)
        assertEquals(AppRoute.Tasks, primaryTabs[1].route)
        assertEquals(AppRoute.Finance, primaryTabs[2].route)
        assertEquals(AppRoute.Calendar, primaryTabs[3].route)
        assertEquals(AppRoute.Assistant, primaryTabs[4].route)
    }

    @Test
    fun `legacy aliases remain mapped for migration safety`() {
        val homeTab = primaryTabs.first { it.route == AppRoute.Home }
        val financeTab = primaryTabs.first { it.route == AppRoute.Finance }
        assertTrue(homeTab.routeAliases.contains(AppRoute.LegacyDashboard))
        assertTrue(financeTab.routeAliases.contains(AppRoute.LegacyExpenses))
    }

    @Test
    fun `onboarding route remains secondary and not a bottom tab`() {
        assertEquals("onboarding", AppRoute.Onboarding)
        assertTrue(primaryTabs.none { it.route == AppRoute.Onboarding })
    }

    @Test
    fun `search route remains secondary and not a bottom tab`() {
        assertEquals("search", AppRoute.Search)
        assertTrue(primaryTabs.none { it.route == AppRoute.Search })
    }
}
