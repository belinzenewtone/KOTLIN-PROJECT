package com.personal.lifeOS.feature.finance.data.repository

import com.personal.lifeOS.feature.finance.domain.model.FinanceSnapshot
import com.personal.lifeOS.feature.finance.domain.model.toFinanceTransaction
import com.personal.lifeOS.feature.finance.domain.repository.FinanceRepository
import com.personal.lifeOS.features.budget.domain.repository.BudgetRepository
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import com.personal.lifeOS.features.income.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepositoryImpl
    @Inject
    constructor(
        private val expenseRepository: ExpenseRepository,
        private val budgetRepository: BudgetRepository,
        private val incomeRepository: IncomeRepository,
    ) : FinanceRepository {
        override fun observeSnapshot(): Flow<FinanceSnapshot> {
            return combine(
            expenseRepository.getAllTransactions(),
            budgetRepository.getBudgets(),
            incomeRepository.getIncomes(),
        ) { transactions, budgets, incomes ->
            FinanceSnapshot(
                transactions = transactions.map { expense -> expense.toFinanceTransaction() },
                budgets = budgets,
                incomes = incomes,
            )
        }
    }
    }
