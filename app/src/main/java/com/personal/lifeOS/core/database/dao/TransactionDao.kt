package com.personal.lifeOS.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.paging.PagingSource
import com.personal.lifeOS.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE user_id = :userId ORDER BY date DESC")
    fun getAllTransactions(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE user_id = :userId OR user_id = '' ORDER BY date DESC")
    suspend fun getAllForSync(userId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id AND user_id = :userId")
    suspend fun getById(
        id: Long,
        userId: String,
    ): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE user_id = :userId AND date BETWEEN :start AND :end ORDER BY date DESC")
    fun getTransactionsBetween(
        start: Long,
        end: Long,
        userId: String,
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE user_id = :userId
          AND deleted_at IS NULL
          AND date BETWEEN :start AND :end
        ORDER BY date DESC
        """,
    )
    suspend fun getTransactionsSnapshot(
        userId: String,
        start: Long,
        end: Long,
    ): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE user_id = :userId AND category = :category ORDER BY date DESC")
    fun getByCategory(
        category: String,
        userId: String,
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT SUM(amount) FROM transactions
        WHERE user_id = :userId
          AND deleted_at IS NULL
          AND date BETWEEN :start AND :end
          AND UPPER(transaction_type) IN ('SENT', 'AIRTIME', 'PAYBILL', 'BUY_GOODS', 'WITHDRAW', 'PAID', 'WITHDRAWN')
        """,
    )
    fun getTotalSpendingBetween(
        start: Long,
        end: Long,
        userId: String,
    ): Flow<Double?>

    @Query(
        """
        SELECT category, SUM(amount) as total
        FROM transactions
        WHERE user_id = :userId
          AND deleted_at IS NULL
          AND date BETWEEN :start AND :end
          AND UPPER(transaction_type) IN ('SENT', 'AIRTIME', 'PAYBILL', 'BUY_GOODS', 'WITHDRAW', 'PAID', 'WITHDRAWN')
        GROUP BY category
        ORDER BY total DESC
    """,
    )
    fun getCategoryBreakdown(
        start: Long,
        end: Long,
        userId: String,
    ): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM transactions WHERE mpesa_code = :code AND user_id = :userId LIMIT 1")
    suspend fun getByMpesaCode(
        code: String,
        userId: String,
    ): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE source_hash = :sourceHash AND user_id = :userId LIMIT 1")
    suspend fun getBySourceHash(
        sourceHash: String,
        userId: String,
    ): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE semantic_hash = :semanticHash AND user_id = :userId LIMIT 1")
    suspend fun getBySemanticHash(semanticHash: String, userId: String): TransactionEntity?

    @Query(
        """
        SELECT * FROM transactions
        WHERE user_id = :userId
          AND ABS(amount - :amount) <= 0.01
          AND date BETWEEN :startTime AND :endTime
          AND UPPER(merchant) = UPPER(:merchant)
        LIMIT 1
        """,
    )
    suspend fun findPotentialDuplicate(
        userId: String,
        amount: Double,
        merchant: String,
        startTime: Long,
        endTime: Long,
    ): TransactionEntity?

    @Query(
        """
        SELECT * FROM transactions
        WHERE user_id = :userId
          AND (merchant LIKE :query OR category LIKE :query)
        ORDER BY date DESC
        LIMIT :limit
        """,
    )
    suspend fun search(
        userId: String,
        query: String,
        limit: Int = 30,
    ): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("SELECT COUNT(*) FROM transactions WHERE user_id = :userId")
    fun getTransactionCount(userId: String): Flow<Int>

    @Query("SELECT * FROM transactions WHERE user_id = :userId AND deleted_at IS NULL ORDER BY date DESC")
    fun pagingSourceAll(userId: String): PagingSource<Int, TransactionEntity>

    @Query(
        """
        SELECT * FROM transactions
        WHERE user_id = :userId AND deleted_at IS NULL
          AND date BETWEEN :start AND :end
        ORDER BY date DESC
        """,
    )
    fun pagingSourceBetween(userId: String, start: Long, end: Long): PagingSource<Int, TransactionEntity>

    @Query(
        """
        SELECT * FROM transactions
        WHERE user_id = :userId AND deleted_at IS NULL
          AND (merchant LIKE :query OR category LIKE :query)
        ORDER BY date DESC
        """,
    )
    fun pagingSourceSearch(userId: String, query: String): PagingSource<Int, TransactionEntity>

    @Query(
        """
        SELECT * FROM transactions
        WHERE user_id = :userId AND deleted_at IS NULL
          AND date BETWEEN :start AND :end
          AND (merchant LIKE :query OR category LIKE :query)
        ORDER BY date DESC
        """,
    )
    fun pagingSourceSearchBetween(
        userId: String,
        start: Long,
        end: Long,
        query: String,
    ): PagingSource<Int, TransactionEntity>

    @Query("UPDATE transactions SET user_id = :userId WHERE user_id = ''")
    suspend fun claimUnownedRecords(userId: String)

    // ── One-shot spending aggregate (suspend) ─────────────────────────────
    // Same semantics as getTotalSpendingBetween but suspend (single-shot) so
    // callers can use it inside async {} parallel blocks without Flow collection.

    @Query(
        """
        SELECT SUM(amount) FROM transactions
        WHERE user_id = :userId
          AND deleted_at IS NULL
          AND date BETWEEN :start AND :end
          AND UPPER(transaction_type) IN ('SENT', 'AIRTIME', 'PAYBILL', 'BUY_GOODS', 'WITHDRAW', 'PAID', 'WITHDRAWN')
        """,
    )
    suspend fun getTotalSpendingBetweenSnapshot(
        start: Long,
        end: Long,
        userId: String,
    ): Double?

    // ── Uncategorized transactions ─────────────────────────────────────────

    /** Count of transactions that still use the default/unknown category. */
    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE user_id = :userId
          AND deleted_at IS NULL
          AND UPPER(category) IN ('OTHER', 'UNCATEGORIZED', 'UNKNOWN', '')
        """,
    )
    fun getUncategorizedCount(userId: String): Flow<Int>

    /** All transactions that still use the default/unknown category. */
    @Query(
        """
        SELECT * FROM transactions
        WHERE user_id = :userId
          AND deleted_at IS NULL
          AND UPPER(category) IN ('OTHER', 'UNCATEGORIZED', 'UNKNOWN', '')
        ORDER BY date DESC
        """,
    )
    fun getUncategorizedTransactions(userId: String): Flow<List<TransactionEntity>>

    // ── Merchant detail ────────────────────────────────────────────────────

    /** All transactions for a specific merchant, newest first. */
    @Query(
        """
        SELECT * FROM transactions
        WHERE user_id = :userId
          AND deleted_at IS NULL
          AND UPPER(merchant) = UPPER(:merchant)
        ORDER BY date DESC
        """,
    )
    fun getTransactionsByMerchant(userId: String, merchant: String): Flow<List<TransactionEntity>>

    // ── Fee analytics ──────────────────────────────────────────────────────

    /** Category breakdown limited to fee-like categories for the fee analytics screen. */
    @Query(
        """
        SELECT category, SUM(amount) as total
        FROM transactions
        WHERE user_id = :userId
          AND deleted_at IS NULL
          AND date BETWEEN :start AND :end
          AND UPPER(category) IN ('AIRTIME', 'FULIZA', 'SUBSCRIPTIONS', 'BANK CHARGES', 'CHARGES', 'FEES')
        GROUP BY category
        ORDER BY total DESC
        """,
    )
    fun getFeeCategoryBreakdown(start: Long, end: Long, userId: String): Flow<List<CategoryTotal>>

    /** Fee-category transactions list for the fee analytics detail screen. */
    @Query(
        """
        SELECT * FROM transactions
        WHERE user_id = :userId
          AND deleted_at IS NULL
          AND date BETWEEN :start AND :end
          AND UPPER(category) IN ('AIRTIME', 'FULIZA', 'SUBSCRIPTIONS', 'BANK CHARGES', 'CHARGES', 'FEES')
        ORDER BY date DESC
        """,
    )
    fun getFeeTransactions(start: Long, end: Long, userId: String): Flow<List<TransactionEntity>>
}

data class CategoryTotal(
    val category: String,
    val total: Double,
)
