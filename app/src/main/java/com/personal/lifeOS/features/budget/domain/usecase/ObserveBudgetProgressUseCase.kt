package com.personal.lifeOS.features.budget.domain.usecase

import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.budget.domain.model.BudgetPeriod
import com.personal.lifeOS.features.budget.domain.model.BudgetProgress
import com.personal.lifeOS.features.budget.domain.repository.BudgetRepository
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class ObserveBudgetProgressUseCase
    @Inject
    constructor(
        private val budgetRepository: BudgetRepository,
        private val expenseRepository: ExpenseRepository,
    ) {
        operator fun invoke(): Flow<List<BudgetProgress>> {
            // Use the widest window (monthly) to fetch all transactions, then filter per budget.
            val start = DateUtils.monthStartMillis()
            val end = DateUtils.monthEndMillis()

            // Compute time boundaries once per invocation, not on every Flow emission
            val zone = ZoneId.systemDefault()
            val todayStart = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
            val todayEnd = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            val weekStart = DateUtils.weekStartMillis()
            val weekEnd = DateUtils.weekEndMillis()

            return combine(
                budgetRepository.getBudgets(),
                expenseRepository.getTransactionsBetween(start, end),
            ) { budgets, transactions ->

                budgets.map { budget ->
                    // Determine the relevant time window based on the budget's period
                    val periodStart = when (budget.period) {
                        BudgetPeriod.DAILY -> todayStart
                        BudgetPeriod.WEEKLY -> weekStart
                        BudgetPeriod.MONTHLY -> start
                    }
                    val periodEnd = when (budget.period) {
                        BudgetPeriod.DAILY -> todayEnd
                        BudgetPeriod.WEEKLY -> weekEnd
                        BudgetPeriod.MONTHLY -> end
                    }
                    val spent = transactions
                        .filter { tx ->
                            tx.category.trim().uppercase() == budget.category.trim().uppercase() &&
                                tx.date in periodStart..periodEnd
                        }
                        .sumOf { it.amount }
                    BudgetProgress(budget = budget, spentAmount = spent)
                }
            }
        }
    }
