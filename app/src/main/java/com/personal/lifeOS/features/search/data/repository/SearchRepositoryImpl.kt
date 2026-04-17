package com.personal.lifeOS.features.search.data.repository

import com.personal.lifeOS.core.database.dao.BudgetDao
import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.dao.IncomeDao
import com.personal.lifeOS.core.database.dao.RecurringRuleDao
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.navigation.AppRoute
import com.personal.lifeOS.features.calendar.domain.model.EventKind
import com.personal.lifeOS.features.search.domain.model.SearchResult
import com.personal.lifeOS.features.search.domain.model.SearchSource
import com.personal.lifeOS.features.search.domain.repository.SearchRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
        private val taskDao: TaskDao,
        private val eventDao: EventDao,
        private val budgetDao: BudgetDao,
        private val incomeDao: IncomeDao,
        private val recurringRuleDao: RecurringRuleDao,
        private val authSessionStore: AuthSessionStore,
    ) : SearchRepository {
        override suspend fun search(
            query: String,
            limitPerSource: Int,
        ): List<SearchResult> {
            val userId = authSessionStore.getUserId()
            if (userId.isBlank()) return emptyList()

            val trimmedQuery = query.trim()
            val likeQuery = "%$trimmedQuery%"
            if (query.isBlank()) return emptyList()

            val results =
                buildList {
                    addAll(searchTransactions(userId, likeQuery, limitPerSource, trimmedQuery))
                    addAll(searchTasks(userId, likeQuery, limitPerSource, trimmedQuery))
                    addAll(searchEvents(userId, likeQuery, limitPerSource, trimmedQuery))
                    addAll(searchBudgets(userId, likeQuery, limitPerSource, trimmedQuery))
                    addAll(searchIncomes(userId, likeQuery, limitPerSource, trimmedQuery))
                    addAll(searchRecurringRules(userId, likeQuery, limitPerSource, trimmedQuery))
                }

            return results.sortedWith(
                compareByDescending<SearchResult> { it.relevanceScore }
                    .thenByDescending { it.timestamp },
            )
        }

        private fun scoreResult(
            query: String,
            primary: String,
            vararg secondary: String,
        ): Int {
            val normalizedQuery = query.trim().lowercase()
            val fields = listOf(primary, *secondary)
                .map { it.lowercase() }

            val exact = fields.any { it == normalizedQuery }
            if (exact) return 400

            val prefix = fields.any { it.startsWith(normalizedQuery) }
            if (prefix) return 250

            val wordPrefix =
                fields.any { candidate ->
                    candidate.split(' ', '-', '_').any { token -> token.startsWith(normalizedQuery) }
                }
            if (wordPrefix) return 160

            val contains = fields.any { it.contains(normalizedQuery) }
            if (contains) return 100

            return 0
        }

        private suspend fun searchTransactions(
            userId: String,
            likeQuery: String,
            limitPerSource: Int,
            trimmedQuery: String,
        ): List<SearchResult> =
            transactionDao.search(userId, likeQuery, limitPerSource).map { tx ->
                SearchResult(
                    id = "tx-${tx.id}",
                    source = SearchSource.TRANSACTION,
                    title = tx.merchant,
                    subtitle = "${tx.category} • ${DateUtils.formatCurrency(tx.amount)}",
                    timestamp = tx.date,
                    relevanceScore = scoreResult(trimmedQuery, tx.merchant, tx.category),
                    navigationTarget = AppRoute.Finance,
                )
            }

        private suspend fun searchTasks(
            userId: String,
            likeQuery: String,
            limitPerSource: Int,
            trimmedQuery: String,
        ): List<SearchResult> =
            taskDao.search(userId, likeQuery, limitPerSource).map { task ->
                SearchResult(
                    id = "task-${task.id}",
                    source = SearchSource.TASK,
                    title = task.title,
                    subtitle = task.description.ifBlank { task.status },
                    timestamp = task.createdAt,
                    relevanceScore = scoreResult(trimmedQuery, task.title, task.description, task.status),
                    navigationTarget = AppRoute.Tasks,
                )
            }

        private suspend fun searchEvents(
            userId: String,
            likeQuery: String,
            limitPerSource: Int,
            trimmedQuery: String,
        ): List<SearchResult> =
            eventDao.search(userId, likeQuery, limitPerSource).map { event ->
                val kindSource = when (runCatching { EventKind.valueOf(event.kind) }.getOrDefault(EventKind.EVENT)) {
                    EventKind.BIRTHDAY -> SearchSource.BIRTHDAY
                    EventKind.ANNIVERSARY -> SearchSource.ANNIVERSARY
                    EventKind.COUNTDOWN -> SearchSource.COUNTDOWN
                    EventKind.EVENT -> SearchSource.EVENT
                }
                SearchResult(
                    id = "event-${event.id}",
                    source = kindSource,
                    title = event.title,
                    subtitle = event.description.ifBlank { event.type },
                    timestamp = event.date,
                    relevanceScore = scoreResult(trimmedQuery, event.title, event.description, event.type),
                    navigationTarget = AppRoute.Calendar,
                )
            }

        private suspend fun searchBudgets(
            userId: String,
            likeQuery: String,
            limitPerSource: Int,
            trimmedQuery: String,
        ): List<SearchResult> =
            budgetDao.search(userId, likeQuery, limitPerSource).map { budget ->
                SearchResult(
                    id = "budget-${budget.id}",
                    source = SearchSource.BUDGET,
                    title = budget.category,
                    subtitle = "Limit ${DateUtils.formatCurrency(budget.limitAmount)}",
                    timestamp = budget.createdAt,
                    relevanceScore = scoreResult(trimmedQuery, budget.category),
                    navigationTarget = AppRoute.Budget,
                )
            }

        private suspend fun searchIncomes(
            userId: String,
            likeQuery: String,
            limitPerSource: Int,
            trimmedQuery: String,
        ): List<SearchResult> =
            incomeDao.search(userId, likeQuery, limitPerSource).map { income ->
                SearchResult(
                    id = "income-${income.id}",
                    source = SearchSource.INCOME,
                    title = income.source,
                    subtitle = DateUtils.formatCurrency(income.amount),
                    timestamp = income.date,
                    relevanceScore = scoreResult(trimmedQuery, income.source),
                    navigationTarget = AppRoute.Income,
                )
            }

        private suspend fun searchRecurringRules(
            userId: String,
            likeQuery: String,
            limitPerSource: Int,
            trimmedQuery: String,
        ): List<SearchResult> =
            recurringRuleDao.search(userId, likeQuery, limitPerSource).map { rule ->
                SearchResult(
                    id = "rule-${rule.id}",
                    source = SearchSource.RECURRING_RULE,
                    title = rule.title,
                    subtitle = "${rule.type} • ${rule.cadence}",
                    timestamp = rule.nextRunAt,
                    relevanceScore = scoreResult(trimmedQuery, rule.title, rule.type, rule.cadence),
                    navigationTarget = AppRoute.Recurring,
                )
            }
    }
