package com.personal.lifeOS.features.recurring.domain.model

data class RecurringRule(
    val id: Long = 0L,
    val title: String,
    val type: RecurringType,
    val cadence: RecurringCadence,
    val nextRunAt: Long,
    val amount: Double? = null,
    val category: String = "RECURRING",
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class RecurringType {
    EXPENSE,
    INCOME,
    TASK,
}

enum class RecurringCadence {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}
