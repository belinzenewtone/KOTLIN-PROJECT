package com.personal.lifeOS.features.recurring.domain

import com.personal.lifeOS.features.recurring.domain.model.RecurringCadence
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class RecurringCadenceCalculatorTest {
    @Test
    fun `daily cadence adds one day`() {
        val input = Instant.parse("2026-03-01T10:00:00Z").toEpochMilli()
        val next = RecurringCadenceCalculator.nextRun(input, RecurringCadence.DAILY)

        val expected = Instant.parse("2026-03-02T10:00:00Z").toEpochMilli()
        assertEquals(expectedInSystemZone(expected), expectedInSystemZone(next))
    }

    @Test
    fun `weekly cadence adds seven days`() {
        val input = Instant.parse("2026-03-01T10:00:00Z").toEpochMilli()
        val next = RecurringCadenceCalculator.nextRun(input, RecurringCadence.WEEKLY)

        val expected = Instant.parse("2026-03-08T10:00:00Z").toEpochMilli()
        assertEquals(expectedInSystemZone(expected), expectedInSystemZone(next))
    }

    @Test
    fun `monthly cadence preserves day when possible`() {
        val input = Instant.parse("2026-01-15T08:30:00Z").toEpochMilli()
        val next = RecurringCadenceCalculator.nextRun(input, RecurringCadence.MONTHLY)

        val expected = Instant.parse("2026-02-15T08:30:00Z").toEpochMilli()
        assertEquals(expectedInSystemZone(expected), expectedInSystemZone(next))
    }

    private fun expectedInSystemZone(epoch: Long): Long {
        return Instant.ofEpochMilli(epoch)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
