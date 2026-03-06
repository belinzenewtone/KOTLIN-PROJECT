package com.personal.lifeOS.features.analytics.data.repository

import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.analytics.domain.model.AnalyticsData
import com.personal.lifeOS.features.analytics.domain.model.CategorySpend
import com.personal.lifeOS.features.analytics.domain.model.DailySpending
import com.personal.lifeOS.features.analytics.domain.repository.AnalyticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val taskDao: TaskDao,
    private val eventDao: EventDao
) : AnalyticsRepository {

    override fun getAnalytics(): Flow<AnalyticsData> {
        val monthStart = DateUtils.monthStartMillis()
        val monthEnd = DateUtils.monthEndMillis()
        val weekStart = DateUtils.weekStartMillis()
        val todayEnd = DateUtils.todayEndMillis()

        val monthTransactions = transactionDao.getTransactionsBetween(monthStart, monthEnd)
        val categoryBreakdown = transactionDao.getCategoryBreakdown(monthStart, monthEnd)
        val pendingTasks = taskDao.getPendingCount()
        val completedTasks = taskDao.getCompletedCountBetween(monthStart, monthEnd)
        val upcomingEvents = eventDao.getUpcomingEvents(System.currentTimeMillis(), 50)

        return combine(
            monthTransactions,
            categoryBreakdown,
            pendingTasks,
            completedTasks,
            upcomingEvents
        ) { transactions, categories, pending, completed, events ->

            // Build daily spending for the week
            val weeklySpending = buildWeeklySpending(transactions, weekStart)

            // Build daily spending for the month
            val monthlySpending = buildMonthlySpending(transactions, monthStart)

            // Category breakdown
            val totalSpent = categories.sumOf { it.total }
            val categorySpends = categories.map { cat ->
                CategorySpend(
                    category = cat.category,
                    amount = cat.total,
                    percentage = if (totalSpent > 0) (cat.total / totalSpent * 100).toFloat() else 0f
                )
            }

            // Productivity score (tasks completed / total tasks * 100)
            val totalTasks = completed + pending
            val productivity = if (totalTasks > 0) (completed.toFloat() / totalTasks * 100) else 0f

            // Average daily spending
            val daysInMonth = LocalDate.now().dayOfMonth
            val avgDaily = if (daysInMonth > 0) totalSpent / daysInMonth else 0.0

            AnalyticsData(
                weeklySpending = weeklySpending,
                monthlySpending = monthlySpending,
                categoryBreakdown = categorySpends,
                productivityScore = productivity,
                totalSpentThisMonth = totalSpent,
                totalTasksCompleted = completed,
                totalTasksPending = pending,
                totalEvents = events.size,
                averageDailySpending = avgDaily
            )
        }
    }

    private fun buildWeeklySpending(
        transactions: List<com.personal.lifeOS.core.database.entity.TransactionEntity>,
        weekStart: Long
    ): List<DailySpending> {
        val result = mutableListOf<DailySpending>()
        for (i in 0..6) {
            val dayStart = weekStart + i * 86400000L
            val dayEnd = dayStart + 86400000L - 1
            val dayTotal = transactions
                .filter { it.date in dayStart..dayEnd }
                .sumOf { it.amount }
            val dayLabel = Instant.ofEpochMilli(dayStart)
                .atZone(ZoneId.systemDefault())
                .dayOfWeek
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            result.add(DailySpending(dayLabel, dayTotal, dayStart))
        }
        return result
    }

    private fun buildMonthlySpending(
        transactions: List<com.personal.lifeOS.core.database.entity.TransactionEntity>,
        monthStart: Long
    ): List<DailySpending> {
        val today = LocalDate.now()
        val result = mutableListOf<DailySpending>()
        for (day in 1..today.dayOfMonth) {
            val date = today.withDayOfMonth(day)
            val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val dayEnd = dayStart + 86400000L - 1
            val dayTotal = transactions
                .filter { it.date in dayStart..dayEnd }
                .sumOf { it.amount }
            result.add(DailySpending(day.toString(), dayTotal, dayStart))
        }
        return result
    }
}
