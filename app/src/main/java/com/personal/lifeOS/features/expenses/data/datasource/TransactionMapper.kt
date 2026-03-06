package com.personal.lifeOS.features.expenses.data.datasource

import com.personal.lifeOS.core.database.entity.TransactionEntity
import com.personal.lifeOS.features.expenses.domain.model.Transaction

fun TransactionEntity.toDomain(): Transaction {
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
        createdAt = createdAt
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        amount = amount,
        merchant = merchant,
        category = category,
        date = date,
        source = source,
        transactionType = transactionType,
        mpesaCode = mpesaCode,
        rawSms = rawSms,
        createdAt = createdAt
    )
}
