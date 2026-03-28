package com.personal.lifeOS.features.assistant.data.datasource

import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.dao.IncomeDao
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.preferences.AppSettingsStore
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.core.utils.DateUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local rule-based AI engine.
 * Parses user queries and returns answers from local data.
 * No cloud API required — works fully offline.
 *
 * Covers: identity, spending, income, savings, categories, tasks, events, comparisons, budgets.
 */
@Singleton
class LocalAIEngine
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
        private val taskDao: TaskDao,
        private val eventDao: EventDao,
        private val incomeDao: IncomeDao,
        private val authSessionStore: AuthSessionStore,
        private val appSettingsStore: AppSettingsStore,
    ) {
        suspend fun processQuery(query: String): String {
            val userId = authSessionStore.getUserId()
            val lower = query.lowercase().trim()
            val userName = appSettingsStore.getProfileName().ifBlank { null }

            return when {
                // Identity / name queries — handle first so they always resolve
                lower.containsAny(
                    "my name", "who am i", "what is my name", "what's my name",
                    "call me", "you know me", "do you know me",
                ) -> handleNameQuery(userName)

                // Greeting — personalised when name is known
                lower.containsAny("hello", "hi", "hey", "good morning", "good evening", "good afternoon") ->
                    if (userName != null) {
                        "Hey $userName! I'm your BELTECH assistant. Ask me about your spending, tasks, income, or schedule."
                    } else {
                        "Hey! I'm your BELTECH assistant. Ask me about your spending, tasks, income, or schedule."
                    }

                // Income queries
                lower.containsAny("income", "earned", "earning", "receive", "received money", "salary") ->
                    handleIncomeQuery(lower, userId)

                // Savings / balance queries
                lower.containsAny("save", "saving", "savings", "balance", "net", "left over", "leftover", "surplus", "deficit") ->
                    handleSavingsQuery(lower, userId)

                // Spending queries
                lower.containsAny("spend", "spent", "expense", "cost", "paid", "pay", "bought") ->
                    handleSpendingQuery(lower, userId)

                // Category queries
                lower.containsAny("category", "categories", "breakdown", "where does my money", "what did i spend on") ->
                    handleCategoryQuery(userId)

                // Biggest/largest expense
                lower.containsAny("biggest", "largest", "most expensive", "highest", "top expense") ->
                    handleLargestExpense(lower, userId)

                // Task queries
                lower.containsAny("task", "todo", "pending", "to do", "to-do", "urgent", "important") ->
                    handleTaskQuery(lower, userId)

                // Event/calendar queries
                lower.containsAny("event", "calendar", "schedule", "meeting", "busy", "plan", "appointment") ->
                    handleEventQuery(lower, userId)

                // Comparison queries
                lower.containsAny("more than", "less than", "compare", "vs", "versus", "last week", "last month") ->
                    handleComparisonQuery(lower, userId)

                // Summary/overview
                lower.containsAny("summary", "overview", "how am i doing", "report", "brief", "update") ->
                    handleSummaryQuery(userId, userName)

                // Help
                lower.containsAny("help", "what can you do", "capabilities", "what do you know") ->
                    buildHelpText()

                else ->
                    "I didn't quite catch that. Try asking about your spending, income, tasks, or schedule — or type **help** to see what I can do."
            }
        }

        private fun handleNameQuery(userName: String?): String {
            return if (userName != null) {
                "Your name is **$userName**. That's what's saved in your profile."
            } else {
                "I don't have your name on file yet. You can set it in your Profile tab."
            }
        }

        private fun buildHelpText(): String {
            return "I can help you with:\n\n" +
                "💰 **Spending** — totals for today, this week, or month\n" +
                "💵 **Income** — what you've earned and from where\n" +
                "📊 **Savings** — your net balance and surplus/deficit\n" +
                "🗂 **Categories** — where your money goes\n" +
                "✅ **Tasks** — pending, urgent, and completed tasks\n" +
                "📅 **Calendar** — upcoming events and busy days\n" +
                "📈 **Trends** — week-on-week spending comparison\n\n" +
                "Just ask in plain language — like \"How much did I spend this week?\" or \"What are my urgent tasks?\""
        }

        private suspend fun handleIncomeQuery(query: String, userId: String): String {
            return try {
                val allIncome = incomeDao.getAll(userId).first()
                when {
                    query.containsAny("today") -> {
                        val today = allIncome.filter {
                            it.date >= DateUtils.todayStartMillis() && it.date <= DateUtils.todayEndMillis()
                        }
                        if (today.isEmpty()) "No income recorded today."
                        else "You received **${DateUtils.formatCurrency(today.sumOf { it.amount })}** today across ${today.size} entr${if (today.size != 1) "ies" else "y"}."
                    }
                    query.containsAny("week", "this week") -> {
                        val week = allIncome.filter { it.date >= DateUtils.weekStartMillis() }
                        if (week.isEmpty()) "No income recorded this week."
                        else "This week you've received **${DateUtils.formatCurrency(week.sumOf { it.amount })}**."
                    }
                    else -> {
                        val month = allIncome.filter {
                            it.date >= DateUtils.monthStartMillis() && it.date <= DateUtils.monthEndMillis()
                        }
                        val total = month.sumOf { it.amount }
                        if (month.isEmpty()) {
                            "No income recorded this month yet."
                        } else {
                            val sources = month.groupBy { it.source }
                                .entries.joinToString("\n") { (src, items) ->
                                    "• **$src**: ${DateUtils.formatCurrency(items.sumOf { it.amount })}"
                                }
                            "This month's income: **${DateUtils.formatCurrency(total)}**\n\n$sources"
                        }
                    }
                }
            } catch (e: Exception) {
                "Income data unavailable right now."
            }
        }

        private suspend fun handleSavingsQuery(query: String, userId: String): String {
            return try {
                val monthSpend = transactionDao.getTotalSpendingBetween(
                    DateUtils.monthStartMillis(), DateUtils.monthEndMillis(), userId,
                ).first() ?: 0.0
                val allIncome = incomeDao.getAll(userId).first()
                val monthIncome = allIncome.filter {
                    it.date >= DateUtils.monthStartMillis() && it.date <= DateUtils.monthEndMillis()
                }.sumOf { it.amount }
                val net = monthIncome - monthSpend
                val direction = if (net >= 0) "surplus" else "deficit"
                val emoji = if (net >= 0) "📈" else "📉"
                "$emoji This month:\n\n" +
                    "Income: **${DateUtils.formatCurrency(monthIncome)}**\n" +
                    "Expenses: **${DateUtils.formatCurrency(monthSpend)}**\n" +
                    "Net **$direction**: **${DateUtils.formatCurrency(kotlin.math.abs(net))}**"
            } catch (e: Exception) {
                "Could not compute savings right now."
            }
        }

        private suspend fun handleSpendingQuery(
            query: String,
            userId: String,
        ): String {
            return when {
                query.containsAny("today") -> {
                    val start = DateUtils.todayStartMillis()
                    val end = DateUtils.todayEndMillis()
                    val total = transactionDao.getTotalSpendingBetween(start, end, userId).first() ?: 0.0
                    val txs = transactionDao.getTransactionsBetween(start, end, userId).first()
                    if (txs.isEmpty()) return "No spending recorded today yet."
                    val topCats = txs.groupBy { it.category }
                        .entries.sortedByDescending { e -> e.value.sumOf { it.amount } }
                        .take(3)
                        .joinToString("\n") { (cat, items) ->
                            "• **$cat**: ${DateUtils.formatCurrency(items.sumOf { it.amount })}"
                        }
                    val topTx = txs.maxByOrNull { it.amount }
                    val topLine = if (topTx != null)
                        "\n\nLargest: **${DateUtils.formatCurrency(topTx.amount)}** at ${topTx.merchant}"
                    else ""
                    "Today you've spent **${DateUtils.formatCurrency(total)}** across ${txs.size} transaction${if (txs.size != 1) "s" else ""}.\n\n$topCats$topLine"
                }
                query.containsAny("week", "this week") -> {
                    val start = DateUtils.weekStartMillis()
                    val end = DateUtils.todayEndMillis()
                    val total = transactionDao.getTotalSpendingBetween(start, end, userId).first() ?: 0.0
                    val txs = transactionDao.getTransactionsBetween(start, end, userId).first()
                    if (txs.isEmpty()) return "No spending recorded this week yet."
                    val catBreakdown = txs.groupBy { it.category }
                        .entries.sortedByDescending { e -> e.value.sumOf { it.amount } }
                        .take(4)
                        .joinToString("\n") { (cat, items) ->
                            val pct = if (total > 0) (items.sumOf { it.amount } / total * 100).toInt() else 0
                            "• **$cat**: ${DateUtils.formatCurrency(items.sumOf { it.amount })} ($pct%)"
                        }
                    val top3Txs = txs.sortedByDescending { it.amount }.take(3)
                        .joinToString("\n") { "• ${it.merchant}: ${DateUtils.formatCurrency(it.amount)}" }
                    "This week you've spent **${DateUtils.formatCurrency(total)}** across ${txs.size} transaction${if (txs.size != 1) "s" else ""}.\n\n" +
                        "**By category:**\n$catBreakdown\n\n" +
                        "**Top transactions:**\n$top3Txs"
                }
                query.containsAny("month", "this month") -> {
                    val start = DateUtils.monthStartMillis()
                    val end = DateUtils.monthEndMillis()
                    val total = transactionDao.getTotalSpendingBetween(start, end, userId).first() ?: 0.0
                    val cats = transactionDao.getCategoryBreakdown(start, end, userId).first()
                    if (cats.isEmpty()) return "No spending recorded this month yet."
                    val catLines = cats.take(5).joinToString("\n") { cat ->
                        val pct = if (total > 0) (cat.total / total * 100).toInt() else 0
                        "• **${cat.category}**: ${DateUtils.formatCurrency(cat.total)} ($pct%)"
                    }
                    val txs = transactionDao.getTransactionsBetween(start, end, userId).first()
                    val topTx = txs.maxByOrNull { it.amount }
                    val topLine = if (topTx != null)
                        "\n\nLargest single expense: **${DateUtils.formatCurrency(topTx.amount)}** at ${topTx.merchant}"
                    else ""
                    "This month you've spent **${DateUtils.formatCurrency(total)}** across ${txs.size} transaction${if (txs.size != 1) "s" else ""}.\n\n" +
                        "**Top categories:**\n$catLines$topLine"
                }
                query.containsAny("food", "restaurant", "eat") -> {
                    val txs = transactionDao.getByCategory("Food", userId).first() +
                        transactionDao.getByCategory("Eating Out", userId).first()
                    val total = txs.sumOf { it.amount }
                    if (txs.isEmpty()) return "No food spending recorded."
                    val topMerchant = txs.groupBy { it.merchant }.maxByOrNull { e -> e.value.sumOf { it.amount } }
                    val topLine = if (topMerchant != null)
                        "\n\nMost spent at: **${topMerchant.key}** (${DateUtils.formatCurrency(topMerchant.value.sumOf { it.amount })})"
                    else ""
                    "You've spent **${DateUtils.formatCurrency(total)}** on food across ${txs.size} transactions.$topLine"
                }
                query.containsAny("transport", "uber", "bolt", "fuel") -> {
                    val txs = transactionDao.getByCategory("Transport", userId).first() +
                        transactionDao.getByCategory("Fuel", userId).first()
                    val total = txs.sumOf { it.amount }
                    if (txs.isEmpty()) return "No transport spending recorded."
                    "You've spent **${DateUtils.formatCurrency(total)}** on transport & fuel across ${txs.size} transactions."
                }
                else -> {
                    val start = DateUtils.monthStartMillis()
                    val end = DateUtils.monthEndMillis()
                    val total = transactionDao.getTotalSpendingBetween(start, end, userId).first() ?: 0.0
                    val cats = transactionDao.getCategoryBreakdown(start, end, userId).first()
                    val topCat = cats.firstOrNull()
                    val hint = if (topCat != null)
                        " Your top category is **${topCat.category}** at ${DateUtils.formatCurrency(topCat.total)}."
                    else ""
                    "This month you've spent **${DateUtils.formatCurrency(total)}**.$hint\n\nAsk about a specific period (today, this week, this month) or category (food, transport) for a detailed breakdown."
                }
            }
        }

        private suspend fun handleCategoryQuery(userId: String): String {
            val categories =
                transactionDao.getCategoryBreakdown(
                    DateUtils.monthStartMillis(),
                    DateUtils.monthEndMillis(),
                    userId,
                ).first()

            if (categories.isEmpty()) return "No transactions recorded this month yet."

            val total = categories.sumOf { it.total }
            val breakdown =
                categories.joinToString("\n") { cat ->
                    val pct = if (total > 0) (cat.total / total * 100).toInt() else 0
                    "• **${cat.category}**: ${DateUtils.formatCurrency(cat.total)} ($pct%)"
                }

            return "Here's your spending breakdown this month:\n\n$breakdown\n\n**Total: ${DateUtils.formatCurrency(
                total,
            )}**"
        }

        private suspend fun handleLargestExpense(
            query: String,
            userId: String,
        ): String {
            val transactions =
                when {
                    query.containsAny("today") ->
                        transactionDao.getTransactionsBetween(
                            DateUtils.todayStartMillis(),
                            DateUtils.todayEndMillis(),
                            userId,
                        ).first()
                    query.containsAny("week") ->
                        transactionDao.getTransactionsBetween(
                            DateUtils.weekStartMillis(),
                            DateUtils.todayEndMillis(),
                            userId,
                        ).first()
                    else ->
                        transactionDao.getTransactionsBetween(
                            DateUtils.monthStartMillis(),
                            DateUtils.monthEndMillis(),
                            userId,
                        ).first()
                }

            if (transactions.isEmpty()) return "No transactions found for that period."

            val largest = transactions.maxByOrNull { it.amount }!!
            return "Your largest expense is **${DateUtils.formatCurrency(
                largest.amount,
            )}** at **${largest.merchant}** (${largest.category}) on ${DateUtils.formatDate(largest.date)}."
        }

        private suspend fun handleTaskQuery(
            query: String,
            userId: String,
        ): String {
            return when {
                query.containsAny("pending", "incomplete", "remaining", "how many") -> {
                    val pending = taskDao.getPendingTasks(userId).first()
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
                    val completed = taskDao.getCompletedTasks(userId).first()
                    "You've completed **${completed.size}** tasks total."
                }
                else -> {
                    val pending = taskDao.getPendingTasks(userId).first().size
                    val completed = taskDao.getCompletedTasks(userId).first().size
                    "📋 **$pending** tasks pending, **$completed** completed."
                }
            }
        }

        private suspend fun handleEventQuery(
            query: String,
            userId: String,
        ): String {
            return when {
                query.containsAny("today") -> {
                    val events =
                        eventDao.getEventsBetween(
                            DateUtils.todayStartMillis(),
                            DateUtils.todayEndMillis(),
                            userId,
                        ).first()
                    if (events.isEmpty()) {
                        "No events scheduled for today."
                    } else {
                        "You have **${events.size}** event${if (events.size != 1) "s" else ""} today:\n\n" +
                            events.joinToString("\n") { "• ${it.title} at ${DateUtils.formatTime(it.date)}" }
                    }
                }
                query.containsAny("week", "this week", "upcoming") -> {
                    val events = eventDao.getUpcomingEvents(System.currentTimeMillis(), userId, 10).first()
                    if (events.isEmpty()) {
                        "No upcoming events scheduled."
                    } else {
                        "Upcoming events:\n\n" +
                            events.joinToString("\n") { "• **${it.title}** — ${DateUtils.formatDate(it.date, "EEE, MMM dd 'at' h:mm a")}" }
                    }
                }
                query.containsAny("busy", "busiest") -> {
                    val events =
                        eventDao.getEventsBetween(
                            DateUtils.weekStartMillis(),
                            DateUtils.todayEndMillis() + 7 * 86400000L,
                            userId,
                        ).first()
                    if (events.isEmpty()) {
                        "Your week looks clear!"
                    } else {
                        val byDay = events.groupBy { DateUtils.formatDate(it.date, "EEEE") }
                        val busiest = byDay.maxByOrNull { it.value.size }
                        "Your busiest day this week is **${busiest?.key}** with ${busiest?.value?.size} events."
                    }
                }
                else -> {
                    val upcoming = eventDao.getUpcomingEvents(System.currentTimeMillis(), userId, 3).first()
                    if (upcoming.isEmpty()) {
                        "No upcoming events."
                    } else {
                        "Next up:\n\n" +
                            upcoming.joinToString("\n") { "• ${it.title} — ${DateUtils.formatDate(it.date, "MMM dd, h:mm a")}" }
                    }
                }
            }
        }

        private suspend fun handleComparisonQuery(
            query: String,
            userId: String,
        ): String {
            val thisWeek =
                transactionDao.getTotalSpendingBetween(
                    DateUtils.weekStartMillis(), DateUtils.todayEndMillis(), userId,
                ).first() ?: 0.0

            // Previous week: 7 days before week start
            val prevWeekStart = DateUtils.weekStartMillis() - 7 * 86400000L
            val prevWeekEnd = DateUtils.weekStartMillis() - 1
            val lastWeek = transactionDao.getTotalSpendingBetween(prevWeekStart, prevWeekEnd, userId).first() ?: 0.0

            return if (lastWeek == 0.0) {
                "This week: **${DateUtils.formatCurrency(thisWeek)}**. No data for last week to compare."
            } else {
                val diff = thisWeek - lastWeek
                val pct = ((diff / lastWeek) * 100).toInt()
                val direction = if (diff > 0) "more 📈" else "less 📉"
                "This week: **${DateUtils.formatCurrency(
                    thisWeek,
                )}**\nLast week: **${DateUtils.formatCurrency(
                    lastWeek,
                )}**\n\nYou're spending **${Math.abs(pct)}% $direction** than last week."
            }
        }

        private suspend fun handleSummaryQuery(userId: String, userName: String?): String {
            val todaySpend = transactionDao.getTotalSpendingBetween(
                DateUtils.todayStartMillis(), DateUtils.todayEndMillis(), userId,
            ).first() ?: 0.0
            val monthSpend = transactionDao.getTotalSpendingBetween(
                DateUtils.monthStartMillis(), DateUtils.monthEndMillis(), userId,
            ).first() ?: 0.0
            val pendingTasks = taskDao.getPendingTasks(userId).first()
            val urgentCount = pendingTasks.count { it.priority.equals("URGENT", ignoreCase = true) }
            val upcomingEvents = eventDao.getUpcomingEvents(System.currentTimeMillis(), userId, 5).first().size
            val monthIncome = try {
                incomeDao.getAll(userId).first()
                    .filter { it.date >= DateUtils.monthStartMillis() && it.date <= DateUtils.monthEndMillis() }
                    .sumOf { it.amount }
            } catch (e: Exception) { 0.0 }
            val net = monthIncome - monthSpend

            val greeting = if (userName != null) "📊 **$userName's BELTECH Summary**" else "📊 **Your BELTECH Summary**"
            val netLine = if (net >= 0) "📈 Net this month: **+${DateUtils.formatCurrency(net)}** surplus"
                         else "📉 Net this month: **-${DateUtils.formatCurrency(kotlin.math.abs(net))}** deficit"
            val urgentLine = if (urgentCount > 0) "\n⚠️ $urgentCount **urgent** task${if (urgentCount != 1) "s" else ""} need attention" else ""

            return "$greeting\n\n" +
                "💰 Today's spending: **${DateUtils.formatCurrency(todaySpend)}**\n" +
                "💰 Month expenses: **${DateUtils.formatCurrency(monthSpend)}**\n" +
                "💵 Month income: **${DateUtils.formatCurrency(monthIncome)}**\n" +
                "$netLine\n" +
                "✅ Pending tasks: **${pendingTasks.size}**$urgentLine\n" +
                "📅 Upcoming events: **$upcomingEvents**"
        }

        private fun String.containsAny(vararg keywords: String): Boolean {
            return keywords.any { this.contains(it, ignoreCase = true) }
        }
    }
