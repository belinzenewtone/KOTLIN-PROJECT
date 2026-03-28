package com.personal.lifeOS.features.review.presentation

import com.personal.lifeOS.features.dashboard.domain.model.DashboardData
import com.personal.lifeOS.features.dashboard.domain.model.DashboardInsight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewViewModelTest {
    @Test
    fun `delta label reflects positive and negative swings`() {
        assertTrue(buildDeltaLabel(1_500.0).contains("Up"))
        assertTrue(buildDeltaLabel(-750.0).contains("Down"))
    }

    @Test
    fun `review ritual prioritizes pending tasks before other advice`() {
        val ritual =
            buildReviewRitual(
                data =
                    DashboardData(
                        pendingTaskCount = 3,
                        insights = listOf(DashboardInsight(1L, "Spend rose", "Review", null, false)),
                    ),
                delta = 2_000.0,
            )

        assertEquals("One thing to do before the week closes", ritual.title)
        assertTrue(ritual.summary.contains("pending task"))
    }

    @Test
    fun `review wins and risks stay populated for sparse and dense weeks`() {
        val sparseWeek = DashboardData()
        val denseWeek =
            DashboardData(
                completedTodayCount = 2,
                pendingTaskCount = 4,
                insights = listOf(DashboardInsight(1L, "Transport climbed", "Watch it", null, false)),
            )

        assertTrue(buildWins(sparseWeek).isNotEmpty())
        assertTrue(buildRisks(sparseWeek, delta = 0.0).isNotEmpty())
        assertTrue(buildWins(denseWeek).first().contains("2"))
        assertTrue(buildRisks(denseWeek, delta = 1_000.0).any { it.contains("4") })
    }
}
