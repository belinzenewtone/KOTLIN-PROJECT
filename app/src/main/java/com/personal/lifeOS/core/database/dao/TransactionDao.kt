package com.personal.lifeOS.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

    @Query("SELECT SUM(amount) FROM transactions WHERE user_id = :userId AND date BETWEEN :start AND :end")
    fun getTotalSpendingBetween(
        start: Long,
        end: Long,
        userId: String,
    ): Flow<Double?>

    @Query(
        """
        SELECT category, SUM(amount) as total 
        FROM transactions 
        WHERE user_id = :userId AND date BETWEEN :start AND :end 
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

    @Query("UPDATE transactions SET user_id = :userId WHERE user_id = ''")
    suspend fun claimUnownedRecords(userId: String)
}

data class CategoryTotal(
    val category: String,
    val total: Double,
)
