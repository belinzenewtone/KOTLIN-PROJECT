package com.personal.lifeOS.features.expenses.data.repository

import com.personal.lifeOS.core.database.LocalIdGenerator
import com.personal.lifeOS.core.database.dao.MerchantCategoryDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.database.entity.MerchantCategoryEntity
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.core.sync.SyncMutationEnqueuer
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
class ExpenseRepositoryImpl
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
        private val merchantCategoryDao: MerchantCategoryDao,
        private val authSessionStore: AuthSessionStore,
        private val syncMutationEnqueuer: SyncMutationEnqueuer,
    ) : ExpenseRepository {
        private fun activeUserId(): String = authSessionStore.getUserId()

        override fun getAllTransactions(): Flow<List<Transaction>> {
            val userId = activeUserId()
            return transactionDao.getAllTransactions(userId).map { entities ->
                entities.map { it.toDomain() }
            }
        }

        override fun getTransactionsBetween(
            start: Long,
            end: Long,
        ): Flow<List<Transaction>> {
            val userId = activeUserId()
            return transactionDao.getTransactionsBetween(start, end, userId).map { entities ->
                entities.map { it.toDomain() }
            }
        }

        override fun getByCategory(category: String): Flow<List<Transaction>> {
            val userId = activeUserId()
            return transactionDao.getByCategory(category, userId).map { entities ->
                entities.map { it.toDomain() }
            }
        }

        override fun getTotalSpendingBetween(
            start: Long,
            end: Long,
        ): Flow<Double> {
            val userId = activeUserId()
            return transactionDao.getTotalSpendingBetween(start, end, userId).map { it ?: 0.0 }
        }

        override fun getCategoryBreakdown(
            start: Long,
            end: Long,
        ): Flow<List<CategoryBreakdown>> {
            val userId = activeUserId()
            return transactionDao.getCategoryBreakdown(start, end, userId).map { totals ->
                val grandTotal = totals.sumOf { it.total }
                totals.map { ct ->
                    CategoryBreakdown(
                        category = ct.category,
                        total = ct.total,
                        percentage = if (grandTotal > 0) (ct.total / grandTotal * 100).toFloat() else 0f,
                    )
                }
            }
        }

        override fun getTransactionCount(): Flow<Int> {
            return transactionDao.getTransactionCount(activeUserId())
        }

        override suspend fun addTransaction(transaction: Transaction): Long {
            val stableId = if (transaction.id > 0L) transaction.id else LocalIdGenerator.nextId()
            transactionDao.insert(
                transaction.toEntity().copy(
                    id = stableId,
                    userId = activeUserId(),
                ),
            )
            syncMutationEnqueuer.enqueueUpsert(
                entityType = "transaction",
                entityId = stableId.toString(),
            )
            return stableId
        }

        override suspend fun updateTransaction(transaction: Transaction) {
            transactionDao.update(
                transaction.toEntity().copy(userId = activeUserId()),
            )
            syncMutationEnqueuer.enqueueUpsert(
                entityType = "transaction",
                entityId = transaction.id.toString(),
            )
        }

        override suspend fun deleteTransaction(transaction: Transaction) {
            transactionDao.delete(
                transaction.toEntity().copy(userId = activeUserId()),
            )
            syncMutationEnqueuer.enqueueDelete(
                entityType = "transaction",
                entityId = transaction.id.toString(),
            )
        }

        override suspend fun getById(id: Long): Transaction? {
            return transactionDao.getById(id, activeUserId())?.toDomain()
        }

        override suspend fun existsByMpesaCode(code: String): Boolean {
            return transactionDao.getByMpesaCode(code, activeUserId()) != null
        }

        override suspend fun existsPotentialDuplicate(
            amount: Double,
            merchant: String,
            date: Long,
            windowMillis: Long,
        ): Boolean {
            val normalizedMerchant = merchant.trim()
            if (normalizedMerchant.isBlank()) return false
            val start = date - windowMillis
            val end = date + windowMillis
            return transactionDao.findPotentialDuplicate(
                userId = activeUserId(),
                amount = amount,
                merchant = normalizedMerchant,
                startTime = start,
                endTime = end,
            ) != null
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
            if (existsPotentialDuplicate(parsed.amount, parsed.merchant, parsed.date)) return null

            // Determine category: check user-corrected first, then auto-categorize
            val category = resolveCategory(parsed.merchant)

            val transaction =
                Transaction(
                    amount = parsed.amount,
                    merchant = parsed.merchant,
                    category = category,
                    date = parsed.date,
                    source = "MPESA",
                    transactionType = parsed.transactionType.name,
                    mpesaCode = parsed.mpesaCode,
                    rawSms = parsed.rawSms,
                )

            val id = addTransaction(transaction)
            return transaction.copy(id = id)
        }

        override suspend fun updateMerchantCategory(
            merchant: String,
            category: String,
        ) {
            val userId = activeUserId()
            val normalizedMerchant = merchant.uppercase()
            val existing = merchantCategoryDao.getByMerchant(normalizedMerchant, userId)
            val stableId = existing?.id ?: LocalIdGenerator.nextId()
            merchantCategoryDao.insert(
                MerchantCategoryEntity(
                    id = stableId,
                    merchant = normalizedMerchant,
                    category = category,
                    confidence = 1.0f,
                    userCorrected = true,
                    userId = userId,
                ),
            )
            syncMutationEnqueuer.enqueueUpsert(
                entityType = "merchant_category",
                entityId = stableId.toString(),
            )
        }

        /**
         * Resolve category: user-corrected mappings take priority over auto-categorization.
         */
        private suspend fun resolveCategory(merchant: String): String {
            // Check if user has corrected this merchant before
            val userMapping = merchantCategoryDao.getByMerchant(merchant.uppercase(), activeUserId())
            if (userMapping != null) return userMapping.category

            // Fall back to auto-categorization
            return MerchantCategorizer.categorize(merchant).label
        }
    }
