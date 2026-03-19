package com.personal.lifeOS.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personal.lifeOS.core.database.entity.InsightCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InsightCardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: InsightCardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<InsightCardEntity>)

    @Query("SELECT * FROM insight_cards WHERE user_id = :userId AND deleted_at IS NULL ORDER BY created_at DESC")
    fun observeAll(userId: String): Flow<List<InsightCardEntity>>

    @Query("DELETE FROM insight_cards WHERE user_id = :userId AND is_ai_generated = 0")
    suspend fun deleteDeterministic(userId: String): Int

    @Query("DELETE FROM insight_cards WHERE user_id = :userId AND fresh_until IS NOT NULL AND fresh_until < :now")
    suspend fun purgeExpired(
        userId: String,
        now: Long = System.currentTimeMillis(),
    ): Int
}
