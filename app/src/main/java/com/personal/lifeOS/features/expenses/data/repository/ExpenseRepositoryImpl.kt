package com.personal.lifeOS.features.expenses.data.repository

import com.personal.lifeOS.core.database.dao.MerchantCategoryDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.database.entity.MerchantCategoryEntity
import com.personal.lifeOS.features.expenses.data.datasource.toDomain
import com.personal.lifeOS.features.expenses.data.datasource.toEntity
import com.personal.lifeOS.features.expenses.data.parser.MerchantCategorizer
import com.personal.lifeOS.features.expenses.data.parser.MpesaSmsParser
import com.personal.lifeOS.features.expenses.domain.model.CategoryBreakdown
import com.personal.lifeOS.features.expenses.domain.model.Transaction
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val merchantCategoryDao: MerchantCategoryDao
) : ExpenseRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTransactionsBetween(start: Long, end: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsBetween(start, end).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getByCategory(category: String): Flow<List<Transaction>> {
        return transactionDao.getByCategory(category).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTotalSpendingBetween(start: Long, end: Long): Flow<Double> {
        return transactionDao.getTotalSpendingBetween(start, end).map { it ?: 0.0 }
    }

    override fun getCategoryBreakdown(start: Long, end: Long): Flow<List<CategoryBreakdown>> {
        return transactionDao.getCategoryBreakdown(start, end).map { totals ->
            val grandTotal = totals.sumOf { it.total }
            totals.map { ct ->
                CategoryBreakdown(
                    category = ct.category,
                    total = ct.total,
                    percentage = if (grandTotal > 0) (ct.total / grandTotal * 100).toFloat() else 0f
                )
            }
        }
    }

    override fun getTransactionCount(): Flow<Int> {
        return transactionDao.getTransactionCount()
    }

    override suspend fun addTransaction(transaction: Transaction): Long {
        return transactionDao.insert(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.delete(transaction.toEntity())
    }

    override suspend fun getById(id: Long): Transaction? {
        return transactionDao.getById(id)?.toDomain()
    }

    override suspend fun existsByMpesaCode(code: String): Boolean {
        return transactionDao.getByMpesaCode(code) != null
    }

    /**
     * Import a transaction from raw SMS text.
     * Parses the SMS, categorizes the merchant, and saves to database.
     * Returns null if SMS is not a valid MPESA message or is a duplicate.
     */
    override suspend fun importFromSms(smsBody: String): Transaction? {
        if (!MpesaSmsParser.isMpesaSms(smsBody)) return null

        val parsed = MpesaSmsParser.parse(smsBody) ?: return null

        // Check for duplicates
        if (existsByMpesaCode(parsed.mpesaCode)) return null

        // Determine category: check user-corrected first, then auto-categorize
        val category = resolveCategory(parsed.merchant)

        val transaction = Transaction(
            amount = parsed.amount,
            merchant = parsed.merchant,
            category = category,
            date = parsed.date,
            source = "MPESA",
            transactionType = parsed.transactionType.name,
            mpesaCode = parsed.mpesaCode,
            rawSms = parsed.rawSms
        )

        val id = addTransaction(transaction)
        return transaction.copy(id = id)
    }

    override suspend fun updateMerchantCategory(merchant: String, category: String) {
        merchantCategoryDao.insert(
            MerchantCategoryEntity(
                merchant = merchant.uppercase(),
                category = category,
                confidence = 1.0f,
                userCorrected = true
            )
        )
    }

    /**
     * Resolve category: user-corrected mappings take priority over auto-categorization.
     */
    private suspend fun resolveCategory(merchant: String): String {
        // Check if user has corrected this merchant before
        val userMapping = merchantCategoryDao.getByMerchant(merchant.uppercase())
        if (userMapping != null) return userMapping.category

        // Fall back to auto-categorization
        return MerchantCategorizer.categorize(merchant).label
    }
}
