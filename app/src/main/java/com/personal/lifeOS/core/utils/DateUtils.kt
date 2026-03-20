package com.personal.lifeOS.core.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object DateUtils {
    fun todayStartMillis(): Long {
        return LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    fun todayEndMillis(): Long {
        return LocalDate.now()
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli() - 1
    }

    fun weekStartMillis(): Long {
        return LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    fun monthStartMillis(): Long {
        return LocalDate.now()
            .withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    fun monthEndMillis(): Long {
        return LocalDate.now()
            .with(TemporalAdjusters.lastDayOfMonth())
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli() - 1
    }

    fun formatDate(
        epochMillis: Long,
        pattern: String = "MMM dd, yyyy",
    ): String {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        val dateTime =
            Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
        return dateTime.format(formatter)
    }

    fun formatTime(epochMillis: Long): String {
        return formatDate(epochMillis, "h:mm a")
    }

    fun formatCurrency(amount: Double): String {
        return "KSh ${String.format("%,.0f", amount)}"
    }
}
