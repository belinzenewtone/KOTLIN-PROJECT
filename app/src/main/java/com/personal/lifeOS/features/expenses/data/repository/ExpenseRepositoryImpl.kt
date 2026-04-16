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
import com.personal.lifeOS.platform.sms.parser.MpesaParsingConfig.TransactionCategory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
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
            return transactionDao.getTotalSpendingBetween(start, end, userId).map { total -> total ?: 0.0 }
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

        override suspend fun existsBySourceHash(sourceHash: String): Boolean {
            return transactionDao.getBySourceHash(sourceHash, activeUserId()) != null
        }

        override suspend fun existsBySemanticHash(semanticHash: String): Boolean {
            return transactionDao.getBySemanticHash(semanticHash, activeUserId()) != null
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

            // Compute source hash for deduplication
            val sourceHash = computeSourceHash(smsBody)

            // Check for duplicates (primary: mpesa_code, secondary: source_hash, tertiary: heuristic)
            if (existsByMpesaCode(parsed.mpesaCode)) return null
            if (existsBySourceHash(sourceHash)) return null
            if (existsPotentialDuplicate(parsed.amount, parsed.merchant, parsed.date)) return null

            // Determine category: user-corrected overrides first, then M-Pesa semantic type,
            // then merchant name lookup. This ensures SENT-to-person shows "Transfer" rather
            // than "Other", and RECEIVED shows "M-Pesa Received", etc.
            val category = resolveCategoryFromMpesa(parsed.merchant, parsed.category)

            val transaction =
                Transaction(
                    amount = parsed.amount,
                    merchant = parsed.merchant,
                    category = category,
                    date = parsed.date,
                    source = "MPESA",
                    transactionType = parsed.category.name,
                    mpesaCode = parsed.mpesaCode,
                    sourceHash = sourceHash,
                    rawSms = parsed.rawSms,
                )

            val id = addTransaction(transaction)
            return transaction.copy(id = id)
        }

        /**
         * Compute SHA-256 hash of raw SMS body for deduplication.
         */
        private fun computeSourceHash(rawMessage: String): String {
            return try {
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                val hashBytes = digest.digest(rawMessage.toByteArray(Charsets.UTF_8))
                hashBytes.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                // Fallback: use string hashCode if SHA-256 fails
                rawMessage.hashCode().toString()
            }
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
         * Resolve category for an M-Pesa imported transaction.
         *
         * Priority:
         *  1. User-corrected merchant mapping (always wins)
         *  2. M-Pesa semantic transaction type (SENT → Transfer, RECEIVED → M-Pesa Received, …)
         *  3. MerchantCategorizer by name (for real businesses on Paybill / Buy Goods)
         *
         * This prevents person-to-person transfers (e.g. "ROSE MATU") from landing in "Other"
         * just because the recipient's name isn't in our merchant dictionary.
         */
        private suspend fun resolveCategoryFromMpesa(
            merchant: String,
            transactionType: TransactionCategory,
        ): String {
            // User-corrected mapping always takes priority
            val userMapping = merchantCategoryDao.getByMerchant(merchant.uppercase(), activeUserId())
            if (userMapping != null) return userMapping.category

            // Map M-Pesa semantic type to a human-readable category
            return when (transactionType) {
                TransactionCategory.SENT -> "Transfer"
                TransactionCategory.RECEIVED -> "M-Pesa Received"
                TransactionCategory.AIRTIME -> "Airtime"
                TransactionCategory.WITHDRAW -> "Withdrawal"
                TransactionCategory.DEPOSIT -> "Deposit"
                TransactionCategory.REVERSED -> "Reversal"
                TransactionCategory.LOAN -> "Loans & Credit"
                TransactionCategory.FULIZA_CHARGE -> "Fuliza Charge"
                // Paybill and Buy Goods are real merchants — let the name lookup handle them
                TransactionCategory.PAYBILL,
                TransactionCategory.BUY_GOODS,
                TransactionCategory.UNKNOWN,
                -> MerchantCategorizer.categorize(merchant).label
            }
        }

        /**
         * Resolve category: user-corrected mappings take priority over auto-categorization.
         * Used for manually-added transactions (no M-Pesa type available).
         */
        private suspend fun resolveCategory(merchant: String): String {
            // Check if user has corrected this merchant before
            val userMapping = merchantCategoryDao.getByMerchant(merchant.uppercase(), activeUserId())
            if (userMapping != null) return userMapping.category

            // Fall back to auto-categorization
            return MerchantCategorizer.categorize(merchant).label
        }

        override fun pagedTransactions(
            startMs: Long?,
            endMs: Long?,
            searchQuery: String,
        ): Flow<PagingData<Transaction>> {
            val userId = activeUserId()
            val hasFilter = startMs != null && endMs != null
            val hasSearch = searchQuery.isNotBlank()
            val likeQuery = "%${searchQuery.trim()}%"
            val pager =
                when {
                    hasFilter && hasSearch ->
                        Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) {
                            transactionDao.pagingSourceSearchBetween(userId, startMs!!, endMs!!, likeQuery)
                        }
                    hasFilter ->
                        Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) {
                            transactionDao.pagingSourceBetween(userId, startMs!!, endMs!!)
                        }
                    hasSearch ->
                        Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) {
                            transactionDao.pagingSourceSearch(userId, likeQuery)
                        }
                    else ->
                        Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) {
                            transactionDao.pagingSourceAll(userId)
                        }
                }
            return pager.flow.map { pagingData ->
                pagingData.map { entity -> entity.toDomain() }
            }
        }
    }
