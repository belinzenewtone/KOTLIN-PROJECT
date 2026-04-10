package com.personal.lifeOS.features.assistant.data.datasource

import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.dao.IncomeDao
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.preferences.AppSettingsStore
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.core.utils.DateUtils
import kotlinx.coroutines.flow.first
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds an aggregated context string from local data.
 * This is sent to the assistant proxy so the AI can give intelligent answers.
 *
 * PRIVACY: Only sends summaries and aggregates, never raw SMS messages.
 */
@Singleton
class DataContextBuilder
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
        private val taskDao: TaskDao,
        private val eventDao: EventDao,
        private val incomeDao: IncomeDao,
        private val authSessionStore: AuthSessionStore,
        private val appSettingsStore: AppSettingsStore,
    ) {
        suspend fun buildContext(): String {
            val userId = authSessionStore.getUserId()
            val userName = appSettingsStore.getProfileName().ifBlank { "the user" }
            val sb = StringBuilder()

            // Identity context — always first so the AI knows who it's talking to
            sb.appendLine("=== IDENTITY ===")
            sb.appendLine("User's name: $userName")
            sb.appendLine("User ID: $userId")

            // Spending context
            try {
                val todaySpend =
                    transactionDao.getTotalSpendingBetween(
                        DateUtils.todayStartMillis(), DateUtils.todayEndMillis(), userId,
                    ).first() ?: 0.0

                val weekSpend =
                    transactionDao.getTotalSpendingBetween(
                        DateUtils.weekStartMillis(), DateUtils.todayEndMillis(), userId,
                    ).first() ?: 0.0

                val monthSpend =
                    transactionDao.getTotalSpendingBetween(
                        DateUtils.monthStartMillis(), DateUtils.monthEndMillis(), userId,
                    ).first() ?: 0.0

                val categories =
                    transactionDao.getCategoryBreakdown(
                        DateUtils.monthStartMillis(),
                        DateUtils.monthEndMillis(),
                        userId,
                    ).first()

                val recentTransactions =
                    transactionDao.getTransactionsBetween(
                        DateUtils.todayStartMillis() - 7 * 86400000L,
                        DateUtils.todayEndMillis(),
                        userId,
                    ).first().take(15)

                sb.appendLine("=== SPENDING ===")
                sb.appendLine("Today: KES ${formatKes(todaySpend)}")
                sb.appendLine("This week: KES ${formatKes(weekSpend)}")
                sb.appendLine("This month: KES ${formatKes(monthSpend)}")

                if (categories.isNotEmpty()) {
                    sb.appendLine("Category breakdown this month:")
                    categories.forEach { sb.appendLine("  ${it.category}: KES ${formatKes(it.total)}") }
                }

                if (recentTransactions.isNotEmpty()) {
                    sb.appendLine("Recent transactions (last 7 days):")
                    recentTransactions.forEach {
                        sb.appendLine(
                            "  ${it.merchant} - KES ${String.format(
                                Locale.getDefault(),
                                "%,.0f",
                                it.amount,
                            )} (${it.category}, ${DateUtils.formatDate(it.date)})",
                        )
                    }
                }

                val txCount = transactionDao.getTransactionCount(userId).first()
                sb.appendLine("Total transactions recorded: $txCount")
            } catch (e: Exception) {
                sb.appendLine("Spending data unavailable")
            }

            // Income context
            try {
                val allIncome = incomeDao.getAll(userId).first()
                val monthIncome = allIncome.filter {
                    it.date >= DateUtils.monthStartMillis() && it.date <= DateUtils.monthEndMillis()
                }
                val totalMonthIncome = monthIncome.sumOf { it.amount }
                val totalAllTime = allIncome.sumOf { it.amount }

                sb.appendLine()
                sb.appendLine("=== INCOME ===")
                sb.appendLine("This month income: KES ${formatKes(totalMonthIncome)}")
                sb.appendLine("All-time income recorded: KES ${formatKes(totalAllTime)}")
                sb.appendLine("Income entries this month: ${monthIncome.size}")

                if (monthIncome.isNotEmpty()) {
                    sb.appendLine("Recent income:")
                    monthIncome.take(5).forEach {
                        sb.appendLine("  ${it.source} - KES ${formatKes(it.amount)} (${DateUtils.formatDate(it.date)})")
                    }
                }
            } catch (e: Exception) {
                sb.appendLine("Income data unavailable")
            }

            // Tasks context
            try {
                val pending = taskDao.getPendingTasks(userId).first()
                val completed = taskDao.getCompletedTasks(userId).first()

                sb.appendLine()
                sb.appendLine("=== TASKS ===")
                sb.appendLine("Pending: ${pending.size}")
                sb.appendLine("Completed: ${completed.size}")

                if (pending.isNotEmpty()) {
                    sb.appendLine("Pending tasks:")
                    pending.take(10).forEach {
                        val deadline = it.deadline?.let { d -> " (due: ${DateUtils.formatDate(d)})" } ?: ""
                        sb.appendLine("  [${it.priority}] ${it.title}$deadline")
                    }
                }
            } catch (e: Exception) {
                sb.appendLine("Task data unavailable")
            }

            // Events context
            try {
                val upcoming = eventDao.getUpcomingEvents(System.currentTimeMillis(), userId, 10).first()

                sb.appendLine()
                sb.appendLine("=== UPCOMING EVENTS ===")
                if (upcoming.isNotEmpty()) {
                    upcoming.forEach {
                        sb.appendLine(
                            "  ${it.title} - ${DateUtils.formatDate(it.date, "EEE MMM dd, h:mm a")} (${it.type})",
                        )
                    }
                } else {
                    sb.appendLine("No upcoming events")
                }
            } catch (e: Exception) {
                sb.appendLine("Event data unavailable")
            }

            sb.appendLine()
            sb.appendLine(
                "Current date: ${DateUtils.formatDate(System.currentTimeMillis(), "EEEE, MMMM dd, yyyy h:mm a")}",
            )

            return sb.toString()
        }

        private fun formatKes(amount: Double): String {
            return String.format(Locale.getDefault(), "%,.0f", amount)
        }
    }
