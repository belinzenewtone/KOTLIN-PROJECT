package com.personal.lifeOS.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personal.lifeOS.core.database.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: IncomeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<IncomeEntity>)

    @Update
    suspend fun update(item: IncomeEntity)

    @Delete
    suspend fun delete(item: IncomeEntity)

    @Query("SELECT * FROM incomes WHERE user_id = :userId ORDER BY date DESC")
    fun getAll(userId: String): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE user_id = :userId OR user_id = '' ORDER BY date DESC")
    suspend fun getAllForSync(userId: String): List<IncomeEntity>

    @Query(
        """
        SELECT * FROM incomes
        WHERE user_id = :userId
          AND (source LIKE :query OR note LIKE :query)
        ORDER BY date DESC
        LIMIT :limit
        """,
    )
    suspend fun search(
        userId: String,
        query: String,
        limit: Int = 30,
    ): List<IncomeEntity>

    @Query("DELETE FROM incomes WHERE id = :id AND user_id = :userId")
    suspend fun deleteById(
        id: Long,
        userId: String,
    )

    @Query("DELETE FROM incomes WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("UPDATE incomes SET user_id = :userId WHERE user_id = ''")
    suspend fun claimUnownedRecords(userId: String)

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0) FROM incomes
        WHERE user_id = :userId
          AND deleted_at IS NULL
          AND date BETWEEN :start AND :end
        """,
    )
    fun getTotalIncomeBetween(
        userId: String,
        start: Long,
        end: Long,
    ): Flow<Double>
}
