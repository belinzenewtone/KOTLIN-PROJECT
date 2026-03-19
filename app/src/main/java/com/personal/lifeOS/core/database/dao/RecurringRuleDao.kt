package com.personal.lifeOS.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personal.lifeOS.core.database.entity.RecurringRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RecurringRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RecurringRuleEntity>)

    @Update
    suspend fun update(item: RecurringRuleEntity)

    @Delete
    suspend fun delete(item: RecurringRuleEntity)

    @Query("SELECT * FROM recurring_rules WHERE user_id = :userId ORDER BY next_run_at ASC")
    fun getAll(userId: String): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE user_id = :userId OR user_id = '' ORDER BY next_run_at ASC")
    suspend fun getAllForSync(userId: String): List<RecurringRuleEntity>

    @Query(
        """
        SELECT * FROM recurring_rules
        WHERE user_id = :userId
          AND enabled = 1
          AND next_run_at <= :now
        ORDER BY next_run_at ASC
        LIMIT :limit
        """,
    )
    suspend fun getDueRules(
        userId: String,
        now: Long,
        limit: Int = 50,
    ): List<RecurringRuleEntity>

    @Query(
        """
        SELECT * FROM recurring_rules
        WHERE user_id = :userId
          AND title LIKE :query
        ORDER BY next_run_at ASC
        LIMIT :limit
        """,
    )
    suspend fun search(
        userId: String,
        query: String,
        limit: Int = 30,
    ): List<RecurringRuleEntity>

    @Query("DELETE FROM recurring_rules WHERE id = :id AND user_id = :userId")
    suspend fun deleteById(
        id: Long,
        userId: String,
    )

    @Query("UPDATE recurring_rules SET enabled = :enabled WHERE id = :id AND user_id = :userId")
    suspend fun setEnabled(
        id: Long,
        enabled: Boolean,
        userId: String,
    )

    @Query("UPDATE recurring_rules SET next_run_at = :nextRunAt WHERE id = :id AND user_id = :userId")
    suspend fun updateNextRunAt(
        id: Long,
        nextRunAt: Long,
        userId: String,
    )

    @Query(
        """
        UPDATE recurring_rules
        SET next_run_at = :nextRunAt
        WHERE id = :id
          AND user_id = :userId
          AND next_run_at = :expectedCurrentRunAt
          AND enabled = 1
        """,
    )
    suspend fun advanceIfDue(
        id: Long,
        userId: String,
        expectedCurrentRunAt: Long,
        nextRunAt: Long,
    ): Int

    @Query("DELETE FROM recurring_rules WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("UPDATE recurring_rules SET user_id = :userId WHERE user_id = ''")
    suspend fun claimUnownedRecords(userId: String)
}
