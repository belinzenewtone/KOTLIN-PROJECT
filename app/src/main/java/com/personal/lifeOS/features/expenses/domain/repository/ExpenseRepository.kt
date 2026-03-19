package com.personal.lifeOS.features.expenses.domain.repository

import com.personal.lifeOS.features.expenses.domain.model.CategoryBreakdown
import com.personal.lifeOS.features.expenses.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getAllTransactions(): Flow<List<Transaction>>

    fun getTransactionsBetween(
        start: Long,
        end: Long,
    ): Flow<List<Transaction>>

    fun getByCategory(category: String): Flow<List<Transaction>>

    fun getTotalSpendingBetween(
        start: Long,
        end: Long,
    ): Flow<Double>

    fun getCategoryBreakdown(
        start: Long,
        end: Long,
    ): Flow<List<CategoryBreakdown>>

    fun getTransactionCount(): Flow<Int>

    suspend fun addTransaction(transaction: Transaction): Long

    suspend fun updateTransaction(transaction: Transaction)

    suspend fun deleteTransaction(transaction: Transaction)

    suspend fun getById(id: Long): Transaction?

    suspend fun existsByMpesaCode(code: String): Boolean

    suspend fun existsPotentialDuplicate(
        amount: Double,
        merchant: String,
        date: Long,
        windowMillis: Long = 5 * 60 * 1000L,
    ): Boolean

    suspend fun importFromSms(smsBody: String): Transaction?

    suspend fun updateMerchantCategory(
        merchant: String,
        category: String,
    )
}
