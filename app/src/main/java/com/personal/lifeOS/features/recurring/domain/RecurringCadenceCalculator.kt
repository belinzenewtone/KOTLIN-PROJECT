package com.personal.lifeOS.features.recurring.domain

import com.personal.lifeOS.features.recurring.domain.model.RecurringCadence
import java.time.Instant
import java.time.ZoneId

object RecurringCadenceCalculator {
    fun nextRun(
        currentRunAt: Long,
        cadence: RecurringCadence,
    ): Long {
        val zone = ZoneId.systemDefault()
        val current = Instant.ofEpochMilli(currentRunAt).atZone(zone)
        val next =
            when (cadence) {
                RecurringCadence.DAILY -> current.plusDays(1)
                RecurringCadence.WEEKLY -> current.plusWeeks(1)
                RecurringCadence.MONTHLY -> current.plusMonths(1)
            }
        return next.toInstant().toEpochMilli()
    }
}
