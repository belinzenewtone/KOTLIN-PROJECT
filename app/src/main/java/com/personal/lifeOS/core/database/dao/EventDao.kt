package com.personal.lifeOS.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personal.lifeOS.core.database.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity): Long

    @Update
    suspend fun update(event: EventEntity)

    @Delete
    suspend fun delete(event: EventEntity)

    @Query("SELECT * FROM events WHERE user_id = :userId ORDER BY date ASC")
    fun getAllEvents(userId: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE user_id = :userId OR user_id = '' ORDER BY date ASC")
    suspend fun getAllForSync(userId: String): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id AND user_id = :userId")
    suspend fun getById(
        id: Long,
        userId: String,
    ): EventEntity?

    @Query("SELECT * FROM events WHERE user_id = :userId AND date BETWEEN :start AND :end ORDER BY date ASC")
    fun getEventsBetween(
        start: Long,
        end: Long,
        userId: String,
    ): Flow<List<EventEntity>>

    @Query(
        """
        SELECT * FROM events
        WHERE user_id = :userId
          AND status = 'PENDING'
          AND date >= :now
        ORDER BY date ASC
        LIMIT :limit
        """,
    )
    fun getUpcomingEvents(
        now: Long,
        userId: String,
        limit: Int = 5,
    ): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE user_id = :userId AND type = :type ORDER BY date ASC")
    fun getByType(
        type: String,
        userId: String,
    ): Flow<List<EventEntity>>

    @Query(
        """
        SELECT * FROM events
        WHERE user_id = :userId
          AND (title LIKE :query OR description LIKE :query)
        ORDER BY date DESC
        LIMIT :limit
        """,
    )
    suspend fun search(
        userId: String,
        query: String,
        limit: Int = 30,
    ): List<EventEntity>

    @Query("SELECT id FROM events WHERE user_id = :userId AND has_reminder = 1")
    suspend fun getScheduledReminderIds(userId: String): List<Long>

    @Query("DELETE FROM events WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("UPDATE events SET user_id = :userId WHERE user_id = ''")
    suspend fun claimUnownedRecords(userId: String)
}
