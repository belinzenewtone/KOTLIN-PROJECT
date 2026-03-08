package com.personal.lifeOS.features.dashboard.data.repository

import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.dashboard.domain.model.DailySpending
import com.personal.lifeOS.features.dashboard.domain.model.DashboardData
import com.personal.lifeOS.features.dashboard.domain.model.RecentTransaction
import com.personal.lifeOS.features.dashboard.domain.model.UpcomingEvent
import com.personal.lifeOS.features.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
        private val eventDao: EventDao,
        private val taskDao: TaskDao,
        private val authSessionStore: AuthSessionStore,
    ) : DashboardRepository {
        override fun getDashboardData(): Flow<DashboardData> {
            val userId = authSessionStore.getUserId()
            val now = System.currentTimeMillis()
            val todayStart = DateUtils.todayStartMillis()
            val todayEnd = DateUtils.todayEndMillis()
            val weekStart = DateUtils.weekStartMillis()
            val monthStart = DateUtils.monthStartMillis()
            val monthEnd = DateUtils.monthEndMillis()

            val todaySpendingFlow = transactionDao.getTotalSpendingBetween(todayStart, todayEnd, userId)
            val weekSpendingFlow = transactionDao.getTotalSpendingBetween(weekStart, todayEnd, userId)
            val monthSpendingFlow = transactionDao.getTotalSpendingBetween(monthStart, monthEnd, userId)
            val upcomingEventsFlow = eventDao.getUpcomingEvents(now, userId, 5)
            val pendingTasksFlow = taskDao.getPendingCount(userId)
            val completedTodayFlow = taskDao.getCompletedCountBetween(todayStart, todayEnd, userId)
            val recentTxFlow = transactionDao.getTransactionsBetween(weekStart, todayEnd, userId)

            return combine(
                todaySpendingFlow,
                weekSpendingFlow,
                monthSpendingFlow,
                upcomingEventsFlow,
                pendingTasksFlow,
            ) { todaySpend, weekSpend, monthSpend, events, pendingCount ->
                // Intermediate combine — 5 arg limit
                DashboardIntermediate(
                    todaySpend,
                    weekSpend,
                    monthSpend,
                    events.map {
                        UpcomingEvent(it.id, it.title, it.date, it.type)
                    },
                    pendingCount,
                )
            }.combine(completedTodayFlow) { intermediate, completedToday ->
                intermediate to completedToday
            }.combine(recentTxFlow) { (intermediate, completedToday), recentTx ->
                // Build weekly spending data from recent transactions
                val weeklyData =
                    buildWeeklySpending(
                        recentTx.map {
                            RecentTransaction(it.id, it.amount, it.merchant, it.category, it.date)
                        },
                    )

                DashboardData(
                    greeting = getGreeting(),
                    todaySpending = intermediate.todaySpend ?: 0.0,
                    weekSpending = intermediate.weekSpend ?: 0.0,
                    monthSpending = intermediate.monthSpend ?: 0.0,
                    upcomingEvents = intermediate.events,
                    pendingTaskCount = intermediate.pendingCount,
                    completedTodayCount = completedToday,
                    recentTransactions =
                        recentTx.take(5).map {
                            RecentTransaction(it.id, it.amount, it.merchant, it.category, it.date)
                        },
                    weeklySpendingData = weeklyData,
                )
            }
        }

        private fun getGreeting(): String {
            val hour = LocalTime.now().hour
            return when {
                hour < 12 -> "Good Morning"
                hour < 17 -> "Good Afternoon"
                else -> "Good Evening"
            }
        }

        private fun buildWeeklySpending(transactions: List<RecentTransaction>): List<DailySpending> {
            val today = LocalDate.now()
            val zone = ZoneId.systemDefault()

            return (6 downTo 0).map { daysAgo ->
                val date = today.minusDays(daysAgo.toLong())
                val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

                val total =
                    transactions
                        .filter { it.date in dayStart..dayEnd }
                        .sumOf { it.amount }

                DailySpending(
                    dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    amount = total,
                )
            }
        }
    }

private data class DashboardIntermediate(
    val todaySpend: Double?,
    val weekSpend: Double?,
    val monthSpend: Double?,
    val events: List<UpcomingEvent>,
    val pendingCount: Int,
)
