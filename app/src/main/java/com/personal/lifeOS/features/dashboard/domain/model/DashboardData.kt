package com.personal.lifeOS.features.dashboard.domain.model

data class DashboardData(
    val greeting: String = "Good Morning",
    val todaySpending: Double = 0.0,
    val weekSpending: Double = 0.0,
    val monthSpending: Double = 0.0,
    val upcomingEvents: List<UpcomingEvent> = emptyList(),
    val pendingTaskCount: Int = 0,
    val completedTodayCount: Int = 0,
    val recentTransactions: List<RecentTransaction> = emptyList(),
    val weeklySpendingData: List<DailySpending> = emptyList()
)

data class UpcomingEvent(
    val id: Long,
    val title: String,
    val date: Long,
    val type: String
)

data class RecentTransaction(
    val id: Long,
    val amount: Double,
    val merchant: String,
    val category: String,
    val date: Long
)

data class DailySpending(
    val dayLabel: String,
    val amount: Double
)
