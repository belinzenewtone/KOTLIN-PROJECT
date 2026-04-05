package com.personal.lifeOS.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personal.lifeOS.core.database.entity.FulizaLoanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FulizaLoanDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(loan: FulizaLoanEntity): Long

    @Update
    suspend fun update(loan: FulizaLoanEntity)

    @Query("SELECT * FROM fuliza_loans WHERE user_id = :userId AND draw_code = :drawCode LIMIT 1")
    suspend fun getByDrawCode(drawCode: String, userId: String): FulizaLoanEntity?

    /** Returns all non-closed loans ordered by draw date descending. */
    @Query(
        "SELECT * FROM fuliza_loans WHERE user_id = :userId AND status != 'CLOSED' ORDER BY draw_date DESC",
    )
    fun observeOpenLoans(userId: String): Flow<List<FulizaLoanEntity>>

    @Query(
        "SELECT * FROM fuliza_loans WHERE user_id = :userId AND status != 'CLOSED' ORDER BY draw_date ASC",
    )
    suspend fun getOpenLoansOldestFirst(userId: String): List<FulizaLoanEntity>

    /** Net outstanding Fuliza balance across all open / partially-repaid loans. */
    @Query(
        "SELECT COALESCE(SUM(draw_amount_kes - total_repaid_kes), 0.0) " +
            "FROM fuliza_loans WHERE user_id = :userId AND status != 'CLOSED'",
    )
    fun observeNetOutstanding(userId: String): Flow<Double>

    @Query("SELECT * FROM fuliza_loans WHERE user_id = :userId ORDER BY draw_date DESC")
    fun observeAll(userId: String): Flow<List<FulizaLoanEntity>>

    @Query("DELETE FROM fuliza_loans WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)
}
