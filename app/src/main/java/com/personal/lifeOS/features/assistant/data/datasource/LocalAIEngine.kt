package com.personal.lifeOS.features.assistant.data.datasource

import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.utils.DateUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local rule-based AI engine.
 * Parses user queries and returns answers from local data.
 * No cloud API required — works fully offline.
 */
@Singleton
class LocalAIEngine @Inject constructor(
    private val transactionDao: TransactionDao,
    private val taskDao: TaskDao,
    private val eventDao: EventDao
) {
    suspend fun processQuery(query: String): String {
        val lower = query.lowercase().trim()

        return when {
            // Spending queries
            lower.containsAny("spend", "spent", "expense", "cost") -> handleSpendingQuery(lower)

            // Category queries
            lower.containsAny("category", "breakdown", "where does my money") -> handleCategoryQuery()

            // Biggest/largest expense
            lower.containsAny("biggest", "largest", "most expensive", "highest") -> handleLargestExpense(lower)

            // Task queries
            lower.containsAny("task", "todo", "pending", "to do", "to-do") -> handleTaskQuery(lower)

            // Event/calendar queries
            lower.containsAny("event", "calendar", "schedule", "meeting", "busy", "plan") -> handleEventQuery(lower)

            // Comparison queries
            lower.containsAny("more than", "less than", "compare", "vs", "versus") -> handleComparisonQuery(lower)

            // Summary/overview
            lower.containsAny("summary", "overview", "how am i doing", "report") -> handleSummaryQuery()

            // Greeting
            lower.containsAny("hello", "hi", "hey", "good morning", "good evening") ->
                "Hey! I'm your BELTECH assistant. I can help you with your expenses, tasks, and schedule. Try asking me something like \"How much did I spend today?\" or \"What tasks are pending?\""

            // Help
            lower.containsAny("help", "what can you do", "capabilities") ->
                "I can help you with:\n\n" +
                "💰 **Expenses** — spending totals, category breakdowns, biggest expenses\n" +
                "✅ **Tasks** — pending count, completion stats\n" +
                "📅 **Calendar** — upcoming events, schedule overview\n" +
                "📊 **Trends** — compare spending across periods\n\n" +
                "Just ask me in plain language!"

            else ->
                "I'm not sure how to answer that yet. Try asking about your spending, tasks, or upcoming events. Type \"help\" to see what I can do."
        }
    }

    private suspend fun handleSpendingQuery(query: String): String {
        return when {
            query.containsAny("today") -> {
                val total = transactionDao.getTotalSpendingBetween(
                    DateUtils.todayStartMillis(), DateUtils.todayEndMillis()
                ).first() ?: 0.0
                val count = transactionDao.getTransactionsBetween(
                    DateUtils.todayStartMillis(), DateUtils.todayEndMillis()
                ).first().size
                "Today you've spent **${DateUtils.formatCurrency(total)}** across $count transaction${if (count != 1) "s" else ""}."
            }
            query.containsAny("week", "this week") -> {
                val total = transactionDao.getTotalSpendingBetween(
                    DateUtils.weekStartMillis(), DateUtils.todayEndMillis()
                ).first() ?: 0.0
                "This week you've spent **${DateUtils.formatCurrency(total)}**."
            }
            query.containsAny("month", "this month") -> {
                val total = transactionDao.getTotalSpendingBetween(
                    DateUtils.monthStartMillis(), DateUtils.monthEndMillis()
                ).first() ?: 0.0
                "This month you've spent **${DateUtils.formatCurrency(total)}**."
            }
            query.containsAny("food", "restaurant", "eat") -> {
                val txs = transactionDao.getByCategory("Food").first()
                val total = txs.sumOf { it.amount }
                "You've spent **${DateUtils.formatCurrency(total)}** on food across ${txs.size} transactions."
            }
            query.containsAny("transport", "uber", "bolt", "fuel") -> {
                val txs = transactionDao.getByCategory("Transport").first()
                val total = txs.sumOf { it.amount }
                "You've spent **${DateUtils.formatCurrency(total)}** on transport across ${txs.size} transactions."
            }
            else -> {
                val total = transactionDao.getTotalSpendingBetween(
                    DateUtils.monthStartMillis(), DateUtils.monthEndMillis()
                ).first() ?: 0.0
                "This month you've spent **${DateUtils.formatCurrency(total)}**. Ask about a specific period (today, this week) or category (food, transport) for more detail."
            }
        }
    }

    private suspend fun handleCategoryQuery(): String {
        val categories = transactionDao.getCategoryBreakdown(
            DateUtils.monthStartMillis(), DateUtils.monthEndMillis()
        ).first()

        if (categories.isEmpty()) return "No transactions recorded this month yet."

        val total = categories.sumOf { it.total }
        val breakdown = categories.joinToString("\n") { cat ->
            val pct = if (total > 0) (cat.total / total * 100).toInt() else 0
            "• **${cat.category}**: ${DateUtils.formatCurrency(cat.total)} ($pct%)"
        }

        return "Here's your spending breakdown this month:\n\n$breakdown\n\n**Total: ${DateUtils.formatCurrency(total)}**"
    }

    private suspend fun handleLargestExpense(query: String): String {
        val transactions = when {
            query.containsAny("today") -> transactionDao.getTransactionsBetween(
                DateUtils.todayStartMillis(), DateUtils.todayEndMillis()
            ).first()
            query.containsAny("week") -> transactionDao.getTransactionsBetween(
                DateUtils.weekStartMillis(), DateUtils.todayEndMillis()
            ).first()
            else -> transactionDao.getTransactionsBetween(
                DateUtils.monthStartMillis(), DateUtils.monthEndMillis()
            ).first()
        }

        if (transactions.isEmpty()) return "No transactions found for that period."

        val largest = transactions.maxByOrNull { it.amount }!!
        return "Your largest expense is **${DateUtils.formatCurrency(largest.amount)}** at **${largest.merchant}** (${largest.category}) on ${DateUtils.formatDate(largest.date)}."
    }

    private suspend fun handleTaskQuery(query: String): String {
        return when {
            query.containsAny("pending", "incomplete", "remaining", "how many") -> {
                val pending = taskDao.getPendingTasks().first()
                if (pending.isEmpty()) {
                    "You have no pending tasks. Nice work! 🎉"
                } else {
                    val critical = pending.count { it.priority == "CRITICAL" }
                    val high = pending.count { it.priority == "HIGH" }
                    var response = "You have **${pending.size}** pending task${if (pending.size != 1) "s" else ""}."
                    if (critical > 0) response += "\n⚠️ **$critical critical** priority."
                    if (high > 0) response += "\n🔴 **$high high** priority."
                    response += "\n\nTop tasks:\n"
                    response += pending.take(5).joinToString("\n") { "• ${it.title} (${it.priority})" }
                    response
                }
            }
            query.containsAny("completed", "done", "finished") -> {
                val completed = taskDao.getCompletedTasks().first()
                "You've completed **${completed.size}** tasks total."
            }
            else -> {
                val pending = taskDao.getPendingTasks().first().size
                val completed = taskDao.getCompletedTasks().first().size
                "📋 **$pending** tasks pending, **$completed** completed."
            }
        }
    }

    private suspend fun handleEventQuery(query: String): String {
        return when {
            query.containsAny("today") -> {
                val events = eventDao.getEventsBetween(
                    DateUtils.todayStartMillis(), DateUtils.todayEndMillis()
                ).first()
                if (events.isEmpty()) "No events scheduled for today."
                else {
                    "You have **${events.size}** event${if (events.size != 1) "s" else ""} today:\n\n" +
                    events.joinToString("\n") { "• ${it.title} at ${DateUtils.formatTime(it.date)}" }
                }
            }
            query.containsAny("week", "this week", "upcoming") -> {
                val events = eventDao.getUpcomingEvents(System.currentTimeMillis(), 10).first()
                if (events.isEmpty()) "No upcoming events scheduled."
                else {
                    "Upcoming events:\n\n" +
                    events.joinToString("\n") { "• **${it.title}** — ${DateUtils.formatDate(it.date, "EEE, MMM dd 'at' h:mm a")}" }
                }
            }
            query.containsAny("busy", "busiest") -> {
                val events = eventDao.getEventsBetween(
                    DateUtils.weekStartMillis(), DateUtils.todayEndMillis() + 7 * 86400000L
                ).first()
                if (events.isEmpty()) "Your week looks clear!"
                else {
                    val byDay = events.groupBy { DateUtils.formatDate(it.date, "EEEE") }
                    val busiest = byDay.maxByOrNull { it.value.size }
                    "Your busiest day this week is **${busiest?.key}** with ${busiest?.value?.size} events."
                }
            }
            else -> {
                val upcoming = eventDao.getUpcomingEvents(System.currentTimeMillis(), 3).first()
                if (upcoming.isEmpty()) "No upcoming events."
                else {
                    "Next up:\n\n" +
                    upcoming.joinToString("\n") { "• ${it.title} — ${DateUtils.formatDate(it.date, "MMM dd, h:mm a")}" }
                }
            }
        }
    }

    private suspend fun handleComparisonQuery(query: String): String {
        val thisWeek = transactionDao.getTotalSpendingBetween(
            DateUtils.weekStartMillis(), DateUtils.todayEndMillis()
        ).first() ?: 0.0

        // Previous week: 7 days before week start
        val prevWeekStart = DateUtils.weekStartMillis() - 7 * 86400000L
        val prevWeekEnd = DateUtils.weekStartMillis() - 1
        val lastWeek = transactionDao.getTotalSpendingBetween(prevWeekStart, prevWeekEnd).first() ?: 0.0

        return if (lastWeek == 0.0) {
            "This week: **${DateUtils.formatCurrency(thisWeek)}**. No data for last week to compare."
        } else {
            val diff = thisWeek - lastWeek
            val pct = ((diff / lastWeek) * 100).toInt()
            val direction = if (diff > 0) "more 📈" else "less 📉"
            "This week: **${DateUtils.formatCurrency(thisWeek)}**\nLast week: **${DateUtils.formatCurrency(lastWeek)}**\n\nYou're spending **${Math.abs(pct)}% $direction** than last week."
        }
    }

    private suspend fun handleSummaryQuery(): String {
        val todaySpend = transactionDao.getTotalSpendingBetween(
            DateUtils.todayStartMillis(), DateUtils.todayEndMillis()
        ).first() ?: 0.0
        val monthSpend = transactionDao.getTotalSpendingBetween(
            DateUtils.monthStartMillis(), DateUtils.monthEndMillis()
        ).first() ?: 0.0
        val pendingTasks = taskDao.getPendingTasks().first().size
        val upcomingEvents = eventDao.getUpcomingEvents(System.currentTimeMillis(), 5).first().size

        return "📊 **Your BELTECH Summary**\n\n" +
            "💰 Today's spending: **${DateUtils.formatCurrency(todaySpend)}**\n" +
            "💰 This month: **${DateUtils.formatCurrency(monthSpend)}**\n" +
            "✅ Pending tasks: **$pendingTasks**\n" +
            "📅 Upcoming events: **$upcomingEvents**"
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it, ignoreCase = true) }
    }
}
