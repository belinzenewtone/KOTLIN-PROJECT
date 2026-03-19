package com.personal.lifeOS.feature.finance.domain.model

import com.personal.lifeOS.features.expenses.domain.model.Transaction

data class FinanceTransaction(
    val id: Long = 0L,
    val amount: Double,
    val merchant: String,
    val category: String,
    val date: Long,
    val source: String = "MPESA",
    val transactionType: String = "SENT",
    val mpesaCode: String? = null,
    val rawSms: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

internal fun Transaction.toFinanceTransaction(): FinanceTransaction {
    return FinanceTransaction(
        id = id,
        amount = amount,
        merchant = merchant,
        category = category,
        date = date,
        source = source,
        transactionType = transactionType,
        mpesaCode = mpesaCode,
        rawSms = rawSms,
        createdAt = createdAt,
    )
}

internal fun FinanceTransaction.toExpenseTransaction(): Transaction {
    return Transaction(
        id = id,
        amount = amount,
        merchant = merchant,
        category = category,
        date = date,
        source = source,
        transactionType = transactionType,
        mpesaCode = mpesaCode,
        rawSms = rawSms,
        createdAt = createdAt,
    )
}
