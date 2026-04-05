package com.personal.lifeOS.feature.finance.domain.model

import com.personal.lifeOS.features.budget.domain.model.Budget
import com.personal.lifeOS.features.income.domain.model.IncomeRecord

data class FinanceSnapshot(
    val transactions: List<FinanceTransaction> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val incomes: List<IncomeRecord> = emptyList(),
) {
    val totalIncome: Double
        get() = incomes.sumOf { it.amount }

    val totalExpenses: Double
        get() = transactions.filter { it.transactionType.uppercase() in SPENDING_OUTFLOW_TYPES }.sumOf { it.amount }

    val netBalance: Double
        get() = totalIncome - totalExpenses

    val totalBudgetLimit: Double
        get() = budgets.sumOf { it.limitAmount }

    val budgetPressurePercent: Float
        get() = if (totalBudgetLimit <= 0.0) 0f else ((totalExpenses / totalBudgetLimit) * 100f).toFloat()
}

private val SPENDING_OUTFLOW_TYPES =
    setOf("SENT", "AIRTIME", "PAYBILL", "BUY_GOODS", "WITHDRAW", "PAID", "WITHDRAWN")
