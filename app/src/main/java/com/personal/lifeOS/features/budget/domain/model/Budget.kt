package com.personal.lifeOS.features.budget.domain.model

data class Budget(
    val id: Long = 0L,
    val category: String,
    val limitAmount: Double,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class BudgetPeriod {
    WEEKLY,
    MONTHLY,
}

data class BudgetProgress(
    val budget: Budget,
    val spentAmount: Double,
) {
    val remainingAmount: Double = (budget.limitAmount - spentAmount).coerceAtLeast(0.0)
    val usagePercent: Float =
        if (budget.limitAmount <= 0.0) 0f else ((spentAmount / budget.limitAmount) * 100f).toFloat()
}
