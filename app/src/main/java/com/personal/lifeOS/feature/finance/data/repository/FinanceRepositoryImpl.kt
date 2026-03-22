package com.personal.lifeOS.feature.finance.data.repository

import androidx.paging.PagingData
import androidx.paging.map
import com.personal.lifeOS.feature.finance.domain.model.FinanceSnapshot
import com.personal.lifeOS.feature.finance.domain.model.FinanceTransaction
import com.personal.lifeOS.feature.finance.domain.model.FinanceTransactionFilter
import com.personal.lifeOS.feature.finance.domain.model.toFinanceTransaction
import com.personal.lifeOS.feature.finance.domain.repository.FinanceRepository
import com.personal.lifeOS.features.budget.domain.repository.BudgetRepository
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import com.personal.lifeOS.features.income.domain.repository.IncomeRepository
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
class FinanceRepositoryImpl
    @Inject
    constructor(
        private val expenseRepository: ExpenseRepository,
        private val budgetRepository: BudgetRepository,
        private val incomeRepository: IncomeRepository,
    ) : FinanceRepository {
        override fun observeSnapshot(): Flow<FinanceSnapshot> =
            combine(
                expenseRepository.getAllTransactions(),
                budgetRepository.getBudgets(),
                incomeRepository.getIncomes(),
            ) { transactions, budgets, incomes ->
                FinanceSnapshot(
                    transactions = transactions.map { it.toFinanceTransaction() },
                    budgets = budgets,
                    incomes = incomes,
                )
            }

        override fun pagedTransactions(
            filter: FinanceTransactionFilter,
            searchQuery: String,
        ): Flow<PagingData<FinanceTransaction>> {
            val (startMs, endMs) = filterToDateRange(filter)
            return expenseRepository
                .pagedTransactions(startMs, endMs, searchQuery)
                .map { pagingData -> pagingData.map { it.toFinanceTransaction() } }
        }

        private fun filterToDateRange(filter: FinanceTransactionFilter): Pair<Long?, Long?> {
            if (filter == FinanceTransactionFilter.ALL) return Pair(null, null)
            val zone = ZoneId.systemDefault()
            val today = Instant.now().atZone(zone).toLocalDate()
            return when (filter) {
                FinanceTransactionFilter.ALL -> Pair(null, null)
                FinanceTransactionFilter.TODAY -> {
                    val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                    Pair(start, end)
                }
                FinanceTransactionFilter.THIS_WEEK -> {
                    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val start = monday.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                    Pair(start, end)
                }
                FinanceTransactionFilter.THIS_MONTH -> {
                    val start = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
                    val end =
                        today
                            .with(TemporalAdjusters.lastDayOfMonth())
                            .plusDays(1)
                            .atStartOfDay(zone)
                            .toInstant()
                            .toEpochMilli() - 1
                    Pair(start, end)
                }
            }
        }
    }
