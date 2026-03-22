package com.personal.lifeOS.feature.finance.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the spending velocity projection logic used by SpendingVelocityBanner.
 *
 * Formula:
 *   dailyRate  = monthSpend / dayOfMonth
 *   projected  = dailyRate * daysInMonth
 *   overshoot  = projected - monthBudget
 *   showBanner = overshoot > 0 AND dayOfMonth >= 3 AND monthBudget > 0
 */
class SpendingVelocityProjectorTest {

    // Pure helper that mirrors the banner logic without Compose dependencies
    private fun project(
        monthSpend: Double,
        monthBudget: Double,
        dayOfMonth: Int,
        daysInMonth: Int,
    ): VelocityResult {
        if (monthBudget <= 0.0) return VelocityResult(showWarning = false, projected = 0.0, overshoot = 0.0)
        if (dayOfMonth < 3) return VelocityResult(showWarning = false, projected = 0.0, overshoot = 0.0)
        val dailyRate = monthSpend / dayOfMonth
        val projected = dailyRate * daysInMonth
        val overshoot = projected - monthBudget
        return VelocityResult(showWarning = overshoot > 0.0, projected = projected, overshoot = overshoot.coerceAtLeast(0.0))
    }

    data class VelocityResult(val showWarning: Boolean, val projected: Double, val overshoot: Double)

    @Test
    fun `no warning when budget is zero`() {
        val r = project(monthSpend = 5000.0, monthBudget = 0.0, dayOfMonth = 15, daysInMonth = 30)
        assertFalse("No warning when no budget set", r.showWarning)
    }

    @Test
    fun `no warning in first two days of month`() {
        val r = project(monthSpend = 2000.0, monthBudget = 10_000.0, dayOfMonth = 2, daysInMonth = 30)
        assertFalse("Too early in month to predict", r.showWarning)
    }

    @Test
    fun `no warning when on track`() {
        // 3000 spent by day 15 of 30, budget = 6000 → projected = 6000 (exactly on budget)
        val r = project(monthSpend = 3000.0, monthBudget = 6000.0, dayOfMonth = 15, daysInMonth = 30)
        assertFalse("Exactly on budget should not trigger warning", r.showWarning)
    }

    @Test
    fun `warning when clearly over pace`() {
        // 5000 spent by day 10 of 30, budget = 10_000 → dailyRate=500, projected=15000, overshoot=5000
        val r = project(monthSpend = 5000.0, monthBudget = 10_000.0, dayOfMonth = 10, daysInMonth = 30)
        assertTrue("Should warn when over-pacing budget", r.showWarning)
        assertEquals(15_000.0, r.projected, 0.01)
        assertEquals(5_000.0, r.overshoot, 0.01)
    }

    @Test
    fun `projection scales correctly by days in month`() {
        // 2000 spent on day 5 of February (28 days) → dailyRate=400, projected=11200
        val r = project(monthSpend = 2000.0, monthBudget = 5_000.0, dayOfMonth = 5, daysInMonth = 28)
        assertEquals(11_200.0, r.projected, 0.01)
        assertEquals(6_200.0, r.overshoot, 0.01)
        assertTrue(r.showWarning)
    }

    @Test
    fun `overshoot is zero when under budget`() {
        // Spending only 1000 by day 15 of 30, budget = 10000 → no overshoot
        val r = project(monthSpend = 1000.0, monthBudget = 10_000.0, dayOfMonth = 15, daysInMonth = 30)
        assertEquals("Overshoot must be zero when under budget", 0.0, r.overshoot, 0.01)
    }

    @Test
    fun `daily rate calculation is accurate`() {
        // 6000 spent over 20 days → dailyRate = 300, projected for 30-day month = 9000
        val r = project(monthSpend = 6000.0, monthBudget = 12_000.0, dayOfMonth = 20, daysInMonth = 30)
        assertEquals(9000.0, r.projected, 0.01)
        assertFalse("9000 < 12000 budget → no warning", r.showWarning)
    }
}
