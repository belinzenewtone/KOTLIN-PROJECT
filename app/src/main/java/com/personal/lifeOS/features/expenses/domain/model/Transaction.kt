package com.personal.lifeOS.features.expenses.domain.model

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val category: String,
    val date: Long,
    val source: String = "MPESA",
    val transactionType: String = "SENT",
    val mpesaCode: String? = null,
    val sourceHash: String? = null,
    val rawSms: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

data class CategoryBreakdown(
    val category: String,
    val total: Double,
    val percentage: Float = 0f,
)

data class SpendingSummary(
    val todayTotal: Double = 0.0,
    val weekTotal: Double = 0.0,
    val monthTotal: Double = 0.0,
    val transactionCount: Int = 0,
    val categoryBreakdown: List<CategoryBreakdown> = emptyList(),
    val topMerchant: String? = null,
)

enum class TransactionFilter {
    ALL,
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
}
