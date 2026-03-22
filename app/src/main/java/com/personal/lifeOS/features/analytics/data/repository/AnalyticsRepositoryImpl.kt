package com.personal.lifeOS.features.analytics.data.repository

import com.personal.lifeOS.core.database.dao.DailySpendDao
import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.database.entity.DailySpendView
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.analytics.domain.model.AnalyticsData
import com.personal.lifeOS.features.analytics.domain.model.CategorySpend
import com.personal.lifeOS.features.analytics.domain.model.DailySpending
import com.personal.lifeOS.features.analytics.domain.repository.AnalyticsRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

@Singleton
class AnalyticsRepositoryImpl
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
        private val taskDao: TaskDao,
        private val eventDao: EventDao,
        private val dailySpendDao: DailySpendDao,
        private val authSessionStore: AuthSessionStore,
    ) : AnalyticsRepository {
        override fun getAnalytics(): Flow<AnalyticsData> =
            flow {
                val userId = authSessionStore.getUserId()
                val monthStart = DateUtils.monthStartMillis()
                val monthEnd = DateUtils.monthEndMillis()
                val weekStart = DateUtils.weekStartMillis()

                // Convert epoch millis → "YYYY-MM-DD" strings for the view queries
                val weekStartDate = epochToDateStr(weekStart)
                val monthStartDate = epochToDateStr(monthStart)
                val monthEndDate = epochToDateStr(monthEnd)

                // First combine: the two daily_spend view queries (keeps combine under 5 args)
                val spendFlows =
                    combine(
                        dailySpendDao.getDailySpend(userId, weekStartDate, monthEndDate),
                        dailySpendDao.getDailySpend(userId, monthStartDate, monthEndDate),
                    ) { weekRows, monthRows -> Pair(weekRows, monthRows) }

                combine(
                    spendFlows,
                    transactionDao.getCategoryBreakdown(monthStart, monthEnd, userId),
                    taskDao.getPendingCount(userId),
                    taskDao.getCompletedCountBetween(monthStart, monthEnd, userId),
                    eventDao.getUpcomingEvents(System.currentTimeMillis(), userId, 50),
                ) { (weekRows, monthRows), categories, pending, completed, events ->
                    val weeklySpending = buildWeeklySpending(weekRows, weekStart)
                    val monthlySpending = buildMonthlySpending(monthRows, monthStart)
                    val totalSpent = categories.sumOf { it.total }
                    val categorySpends =
                        categories.map { cat ->
                            CategorySpend(
                                category = cat.category,
                                amount = cat.total,
                                percentage =
                                    if (totalSpent > 0) {
                                        (cat.total / totalSpent * 100).toFloat()
                                    } else {
                                        0f
                                    },
                            )
                        }
                    val totalTasks = completed + pending
                    val productivity =
                        if (totalTasks > 0) (completed.toFloat() / totalTasks * 100) else 0f
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
                        averageDailySpending = avgDaily,
                    )
                }.collect { emit(it) }
            }

        // ── Private helpers ───────────────────────────────────────────────────

        private fun epochToDateStr(epochMillis: Long): String =
            Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString() // produces "YYYY-MM-DD"

        /**
         * Builds a 7-day spending list for the current week starting at [weekStart].
         * Fills in zero-spend days for days absent from the view.
         */
        private fun buildWeeklySpending(
            rows: List<DailySpendView>,
            weekStart: Long,
        ): List<DailySpending> {
            val spendByDate = rows.associateBy { it.spendDate }
            return (0..6).map { i ->
                val dayMillis = weekStart + i * 86_400_000L
                val dateStr = epochToDateStr(dayMillis)
                val amount = spendByDate[dateStr]?.totalAmount ?: 0.0
                val dayLabel =
                    Instant.ofEpochMilli(dayMillis)
                        .atZone(ZoneId.systemDefault())
                        .dayOfWeek
                        .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                DailySpending(dayLabel, amount, dayMillis)
            }
        }

        /**
         * Builds a day-by-day spending list for every day in the current month up to today.
         * Fills in zero-spend days for days absent from the view.
         */
        private fun buildMonthlySpending(
            rows: List<DailySpendView>,
            @Suppress("UNUSED_PARAMETER") monthStart: Long,
        ): List<DailySpending> {
            val spendByDate = rows.associateBy { it.spendDate }
            val today = LocalDate.now()
            return (1..today.dayOfMonth).map { day ->
                val date = today.withDayOfMonth(day)
                val dayMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val amount = spendByDate[date.toString()]?.totalAmount ?: 0.0
                DailySpending(day.toString(), amount, dayMillis)
            }
        }
    }
