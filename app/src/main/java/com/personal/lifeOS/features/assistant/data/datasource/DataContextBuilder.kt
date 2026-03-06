package com.personal.lifeOS.features.assistant.data.datasource

import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.utils.DateUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds an aggregated context string from local data.
 * This is sent to OpenAI so the AI can give intelligent answers.
 * 
 * PRIVACY: Only sends summaries and aggregates, never raw SMS messages.
 */
@Singleton
class DataContextBuilder @Inject constructor(
    private val transactionDao: TransactionDao,
    private val taskDao: TaskDao,
    private val eventDao: EventDao
) {
    suspend fun buildContext(): String {
        val sb = StringBuilder()

        // Spending context
        try {
            val todaySpend = transactionDao.getTotalSpendingBetween(
                DateUtils.todayStartMillis(), DateUtils.todayEndMillis()
            ).first() ?: 0.0

            val weekSpend = transactionDao.getTotalSpendingBetween(
                DateUtils.weekStartMillis(), DateUtils.todayEndMillis()
            ).first() ?: 0.0

            val monthSpend = transactionDao.getTotalSpendingBetween(
                DateUtils.monthStartMillis(), DateUtils.monthEndMillis()
            ).first() ?: 0.0

            val categories = transactionDao.getCategoryBreakdown(
                DateUtils.monthStartMillis(), DateUtils.monthEndMillis()
            ).first()

            val recentTransactions = transactionDao.getTransactionsBetween(
                DateUtils.todayStartMillis() - 7 * 86400000L, DateUtils.todayEndMillis()
            ).first().take(15)

            sb.appendLine("=== SPENDING ===")
            sb.appendLine("Today: KES ${String.format("%,.0f", todaySpend)}")
            sb.appendLine("This week: KES ${String.format("%,.0f", weekSpend)}")
            sb.appendLine("This month: KES ${String.format("%,.0f", monthSpend)}")

            if (categories.isNotEmpty()) {
                sb.appendLine("Category breakdown this month:")
                categories.forEach { sb.appendLine("  ${it.category}: KES ${String.format("%,.0f", it.total)}") }
            }

            if (recentTransactions.isNotEmpty()) {
                sb.appendLine("Recent transactions (last 7 days):")
                recentTransactions.forEach {
                    sb.appendLine("  ${it.merchant} - KES ${String.format("%,.0f", it.amount)} (${it.category}, ${DateUtils.formatDate(it.date)})")
                }
            }

            val txCount = transactionDao.getTransactionCount().first()
            sb.appendLine("Total transactions recorded: $txCount")
        } catch (e: Exception) {
            sb.appendLine("Spending data unavailable")
        }

        // Tasks context
        try {
            val pending = taskDao.getPendingTasks().first()
            val completed = taskDao.getCompletedTasks().first()

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
            val upcoming = eventDao.getUpcomingEvents(System.currentTimeMillis(), 10).first()

            sb.appendLine()
            sb.appendLine("=== UPCOMING EVENTS ===")
            if (upcoming.isNotEmpty()) {
                upcoming.forEach {
                    sb.appendLine("  ${it.title} - ${DateUtils.formatDate(it.date, "EEE MMM dd, h:mm a")} (${it.type})")
                }
            } else {
                sb.appendLine("No upcoming events")
            }
        } catch (e: Exception) {
            sb.appendLine("Event data unavailable")
        }

        sb.appendLine()
        sb.appendLine("Current date: ${DateUtils.formatDate(System.currentTimeMillis(), "EEEE, MMMM dd, yyyy h:mm a")}")

        return sb.toString()
    }
}
