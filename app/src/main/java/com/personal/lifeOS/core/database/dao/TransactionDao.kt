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

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY date DESC")
    fun getByCategory(category: String): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE date BETWEEN :start AND :end")
    fun getTotalSpendingBetween(start: Long, end: Long): Flow<Double?>

    @Query("""
        SELECT category, SUM(amount) as total 
        FROM transactions 
        WHERE date BETWEEN :start AND :end 
        GROUP BY category 
        ORDER BY total DESC
    """)
    fun getCategoryBreakdown(start: Long, end: Long): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM transactions WHERE mpesa_code = :code LIMIT 1")
    suspend fun getByMpesaCode(code: String): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions")
    fun getTransactionCount(): Flow<Int>
}

data class CategoryTotal(
    val category: String,
    val total: Double
)
