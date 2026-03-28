package com.personal.lifeOS.feature.home.presentation

import com.personal.lifeOS.features.dashboard.domain.model.DashboardData
import com.personal.lifeOS.features.dashboard.domain.model.DashboardInsight
import com.personal.lifeOS.features.dashboard.domain.model.RecentTransaction
import com.personal.lifeOS.features.dashboard.domain.model.UpcomingEvent
import com.personal.lifeOS.features.dashboard.presentation.DashboardUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeContractsTest {
    @Test
    fun `dashboard state maps into home state contract`() {
        val mapped =
            DashboardUiState(
                data =
                    DashboardData(
                        greeting = "Good Evening",
                        todaySpending = 100.0,
                        weekSpending = 500.0,
                        monthSpending = 1200.0,
                        pendingTaskCount = 3,
                        completedTodayCount = 1,
                        upcomingEvents = listOf(UpcomingEvent(1, "Strategy Sync", System.currentTimeMillis(), "WORK")),
                        recentTransactions =
                            listOf(
                                RecentTransaction(
                                    id = 1,
                                    amount = 200.0,
                                    merchant = "Market",
                                    category = "Food",
                                    date = System.currentTimeMillis(),
                                ),
                            ),
                        insights = listOf(DashboardInsight(1, "Spending rising", "Watch transport", null, false)),
                    ),
                isLoading = false,
                error = null,
            ).toHomeUiState()

        assertEquals("Good Evening", mapped.greeting)
        assertEquals(3, mapped.pendingTaskCount)
        assertEquals(1, mapped.completedTodayCount)
        assertNotNull(mapped.nextEventTitle)
        assertNotNull(mapped.topInsight)
        assertNotNull(mapped.weeklyRitual)
        assertEquals(5, mapped.quickActions.size)
        assertTrue(mapped.syncFreshness?.label?.isNotBlank() != false)
    }
}
