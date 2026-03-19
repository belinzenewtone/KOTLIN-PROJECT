package com.personal.lifeOS.features.analytics.domain.model

data class AnalyticsData(
    val weeklySpending: List<DailySpending> = emptyList(),
    val monthlySpending: List<DailySpending> = emptyList(),
    val categoryBreakdown: List<CategorySpend> = emptyList(),
    val productivityScore: Float = 0f,
    val totalSpentThisMonth: Double = 0.0,
    val totalTasksCompleted: Int = 0,
    val totalTasksPending: Int = 0,
    val totalEvents: Int = 0,
    val averageDailySpending: Double = 0.0,
)

data class DailySpending(
    val dayLabel: String, // "Mon", "Tue" or "1", "2"
    val amount: Double,
    val date: Long,
)

data class CategorySpend(
    val category: String,
    val amount: Double,
    val percentage: Float,
)

enum class AnalyticsPeriod {
    WEEK,
    MONTH,
}
