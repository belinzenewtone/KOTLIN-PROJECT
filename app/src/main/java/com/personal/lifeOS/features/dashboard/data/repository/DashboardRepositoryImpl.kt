package com.personal.lifeOS.features.dashboard.data.repository

import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.database.entity.EventEntity
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.dashboard.domain.model.DailySpending
import com.personal.lifeOS.features.dashboard.domain.model.DashboardData
import com.personal.lifeOS.features.dashboard.domain.model.DashboardInsight
import com.personal.lifeOS.features.dashboard.domain.model.RecentTransaction
import com.personal.lifeOS.features.dashboard.domain.model.UpcomingEvent
import com.personal.lifeOS.features.dashboard.domain.repository.DashboardRepository
import com.personal.lifeOS.features.insights.domain.model.InsightCard
import com.personal.lifeOS.features.insights.domain.repository.InsightRepository
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
        private val insightRepository: InsightRepository,
    ) : DashboardRepository {
        override fun getDashboardData(): Flow<DashboardData> {
            val userId = authSessionStore.getUserId()
            val windows = dashboardWindows()

            val todaySpendingFlow =
                transactionDao.getTotalSpendingBetween(windows.todayStart, windows.todayEnd, userId)
            val weekSpendingFlow =
                transactionDao.getTotalSpendingBetween(windows.weekStart, windows.todayEnd, userId)
            val monthSpendingFlow =
                transactionDao.getTotalSpendingBetween(windows.monthStart, windows.monthEnd, userId)
            val upcomingEventsFlow = eventDao.getUpcomingEvents(windows.now, userId, 5)
            val pendingTasksFlow = taskDao.getPendingCount(userId)
            val completedTodayFlow = taskDao.getCompletedCountBetween(windows.todayStart, windows.todayEnd, userId)
            val recentTxFlow = transactionDao.getTransactionsBetween(windows.weekStart, windows.todayEnd, userId)
            val insightsFlow = insightRepository.observeCards()

            return combine(
                todaySpendingFlow,
                weekSpendingFlow,
                monthSpendingFlow,
                upcomingEventsFlow,
                pendingTasksFlow,
            ) { todaySpend, weekSpend, monthSpend, events, pendingCount ->
                DashboardIntermediate(
                    todaySpend = todaySpend,
                    weekSpend = weekSpend,
                    monthSpend = monthSpend,
                    events = mapUpcomingEvents(events),
                    pendingCount = pendingCount,
                )
            }.combine(completedTodayFlow) { intermediate, completedToday ->
                intermediate to completedToday
            }.combine(recentTxFlow) { (intermediate, completedToday), recentTx ->
                val mappedRecentTransactions =
                    recentTx.map {
                        RecentTransaction(
                            id = it.id,
                            amount = it.amount,
                            merchant = it.merchant,
                            category = it.category,
                            date = it.date,
                        )
                    }

                DashboardWithTransactions(
                    intermediate = intermediate,
                    completedToday = completedToday,
                    recentTransactions = mappedRecentTransactions,
                    weeklySpending = buildWeeklySpending(mappedRecentTransactions),
                )
            }.combine(insightsFlow) { withTransactions, insights ->
                DashboardData(
                    greeting = getGreeting(),
                    todaySpending = withTransactions.intermediate.todaySpend ?: 0.0,
                    weekSpending = withTransactions.intermediate.weekSpend ?: 0.0,
                    monthSpending = withTransactions.intermediate.monthSpend ?: 0.0,
                    upcomingEvents = withTransactions.intermediate.events,
                    pendingTaskCount = withTransactions.intermediate.pendingCount,
                    completedTodayCount = withTransactions.completedToday,
                    recentTransactions = withTransactions.recentTransactions.take(5),
                    weeklySpendingData = withTransactions.weeklySpending,
                    insights = mapDashboardInsights(insights),
                )
            }
        }

        private fun dashboardWindows(): DashboardWindows {
            return DashboardWindows(
                now = System.currentTimeMillis(),
                todayStart = DateUtils.todayStartMillis(),
                todayEnd = DateUtils.todayEndMillis(),
                weekStart = DateUtils.weekStartMillis(),
                monthStart = DateUtils.monthStartMillis(),
                monthEnd = DateUtils.monthEndMillis(),
            )
        }

        private fun mapUpcomingEvents(events: List<EventEntity>): List<UpcomingEvent> {
            return events.map { event ->
                UpcomingEvent(
                    id = event.id,
                    title = event.title,
                    date = event.date,
                    type = event.type,
                )
            }
        }

        private fun mapDashboardInsights(insights: List<InsightCard>): List<DashboardInsight> {
            return insights
                .take(3)
                .map { card ->
                    DashboardInsight(
                        id = card.id,
                        title = card.title,
                        body = card.body,
                        confidence = card.confidence,
                        isAiGenerated = card.isAiGenerated,
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

private data class DashboardWithTransactions(
    val intermediate: DashboardIntermediate,
    val completedToday: Int,
    val recentTransactions: List<RecentTransaction>,
    val weeklySpending: List<DailySpending>,
)

private data class DashboardWindows(
    val now: Long,
    val todayStart: Long,
    val todayEnd: Long,
    val weekStart: Long,
    val monthStart: Long,
    val monthEnd: Long,
)
