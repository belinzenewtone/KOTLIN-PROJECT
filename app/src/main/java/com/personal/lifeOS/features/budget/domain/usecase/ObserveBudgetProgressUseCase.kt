package com.personal.lifeOS.features.budget.domain.usecase

import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.budget.domain.model.BudgetProgress
import com.personal.lifeOS.features.budget.domain.repository.BudgetRepository
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveBudgetProgressUseCase
    @Inject
    constructor(
        private val budgetRepository: BudgetRepository,
        private val expenseRepository: ExpenseRepository,
    ) {
        operator fun invoke(): Flow<List<BudgetProgress>> {
            val monthStart = DateUtils.monthStartMillis()
            val monthEnd = DateUtils.monthEndMillis()

            return combine(
                budgetRepository.getBudgets(),
                expenseRepository.getTransactionsBetween(monthStart, monthEnd),
            ) { budgets, transactions ->
                val spentByCategory =
                    transactions
                        .groupBy { tx -> tx.category.trim().uppercase() }
                        .mapValues { (_, list) -> list.sumOf { tx -> tx.amount } }

                budgets.map { budget ->
                    BudgetProgress(
                        budget = budget,
                        spentAmount = spentByCategory[budget.category.trim().uppercase()] ?: 0.0,
                    )
                }
            }
        }
    }
