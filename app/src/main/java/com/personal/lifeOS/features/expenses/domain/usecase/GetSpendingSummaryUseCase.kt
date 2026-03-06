package com.personal.lifeOS.features.expenses.domain.usecase

import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.expenses.domain.model.SpendingSummary
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetSpendingSummaryUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    operator fun invoke(): Flow<SpendingSummary> {
        val todaySpending = repository.getTotalSpendingBetween(
            DateUtils.todayStartMillis(), DateUtils.todayEndMillis()
        )
        val weekSpending = repository.getTotalSpendingBetween(
            DateUtils.weekStartMillis(), DateUtils.todayEndMillis()
        )
        val monthSpending = repository.getTotalSpendingBetween(
            DateUtils.monthStartMillis(), DateUtils.monthEndMillis()
        )
        val categoryBreakdown = repository.getCategoryBreakdown(
            DateUtils.monthStartMillis(), DateUtils.monthEndMillis()
        )
        val txCount = repository.getTransactionCount()

        return combine(
            todaySpending,
            weekSpending,
            monthSpending,
            categoryBreakdown,
            txCount
        ) { today, week, month, categories, count ->
            SpendingSummary(
                todayTotal = today,
                weekTotal = week,
                monthTotal = month,
                transactionCount = count,
                categoryBreakdown = categories,
                topMerchant = null // Could be computed from additional query
            )
        }
    }
}
