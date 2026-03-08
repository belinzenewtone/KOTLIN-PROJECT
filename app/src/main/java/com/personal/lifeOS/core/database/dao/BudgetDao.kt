package com.personal.lifeOS.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personal.lifeOS.core.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BudgetEntity>)

    @Update
    suspend fun update(item: BudgetEntity)

    @Delete
    suspend fun delete(item: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE user_id = :userId ORDER BY category ASC")
    fun getAll(userId: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE user_id = :userId OR user_id = '' ORDER BY category ASC")
    suspend fun getAllForSync(userId: String): List<BudgetEntity>

    @Query(
        """
        SELECT * FROM budgets
        WHERE user_id = :userId
          AND category LIKE :query
        ORDER BY category ASC
        LIMIT :limit
        """,
    )
    suspend fun search(
        userId: String,
        query: String,
        limit: Int = 30,
    ): List<BudgetEntity>

    @Query("DELETE FROM budgets WHERE id = :id AND user_id = :userId")
    suspend fun deleteById(
        id: Long,
        userId: String,
    )

    @Query("DELETE FROM budgets WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("UPDATE budgets SET user_id = :userId WHERE user_id = ''")
    suspend fun claimUnownedRecords(userId: String)
}
