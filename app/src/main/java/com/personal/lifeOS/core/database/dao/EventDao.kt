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

    @Query("SELECT * FROM events ORDER BY date ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: Long): EventEntity?

    @Query("SELECT * FROM events WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    fun getEventsBetween(start: Long, end: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE date >= :now ORDER BY date ASC LIMIT :limit")
    fun getUpcomingEvents(now: Long, limit: Int = 5): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE type = :type ORDER BY date ASC")
    fun getByType(type: String): Flow<List<EventEntity>>
}
