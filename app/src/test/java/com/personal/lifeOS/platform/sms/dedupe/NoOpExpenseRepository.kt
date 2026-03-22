package com.personal.lifeOS.platform.sms.dedupe

import androidx.paging.PagingData
import com.personal.lifeOS.features.expenses.domain.model.CategoryBreakdown
import com.personal.lifeOS.features.expenses.domain.model.Transaction
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** No-op stub used by tests that only exercise pure computation on the engine. */
internal class NoOpExpenseRepository : ExpenseRepository {
    override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
    override fun getTransactionsBetween(start: Long, end: Long): Flow<List<Transaction>> = flowOf(emptyList())
    override fun getByCategory(category: String): Flow<List<Transaction>> = flowOf(emptyList())
    override fun getTotalSpendingBetween(start: Long, end: Long): Flow<Double> = flowOf(0.0)
    override fun getCategoryBreakdown(start: Long, end: Long): Flow<List<CategoryBreakdown>> = flowOf(emptyList())
    override fun getTransactionCount(): Flow<Int> = flowOf(0)
    override suspend fun addTransaction(transaction: Transaction): Long = 0L
    override suspend fun updateTransaction(transaction: Transaction) = Unit
    override suspend fun deleteTransaction(transaction: Transaction) = Unit
    override suspend fun getById(id: Long): Transaction? = null
    override suspend fun existsByMpesaCode(code: String) = false
    override suspend fun existsBySourceHash(hash: String) = false
    override suspend fun existsBySemanticHash(hash: String) = false
    override suspend fun existsPotentialDuplicate(amount: Double, merchant: String, date: Long, windowMillis: Long) = false
    override suspend fun importFromSms(smsBody: String): Transaction? = null
    override suspend fun updateMerchantCategory(merchant: String, category: String) = Unit
    override fun pagedTransactions(startMs: Long?, endMs: Long?, searchQuery: String): Flow<PagingData<Transaction>> =
        flowOf(PagingData.empty())
}
