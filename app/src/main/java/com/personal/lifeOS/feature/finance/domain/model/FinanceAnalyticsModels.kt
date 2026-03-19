package com.personal.lifeOS.feature.finance.domain.model

data class FinanceCategoryBreakdown(
    val category: String,
    val total: Double,
    val percentage: Float = 0f,
)

data class FinanceSpendingSummary(
    val todayTotal: Double = 0.0,
    val weekTotal: Double = 0.0,
    val monthTotal: Double = 0.0,
    val transactionCount: Int = 0,
    val categoryBreakdown: List<FinanceCategoryBreakdown> = emptyList(),
    val topMerchant: String? = null,
)

enum class FinanceTransactionFilter {
    ALL,
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
}
