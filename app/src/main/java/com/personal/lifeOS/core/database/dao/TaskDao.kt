package com.personal.lifeOS.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personal.lifeOS.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE user_id = :userId ORDER BY deadline ASC")
    fun getAllTasks(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE user_id = :userId OR user_id = '' ORDER BY deadline ASC")
    suspend fun getAllForSync(userId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id AND user_id = :userId")
    suspend fun getById(
        id: Long,
        userId: String,
    ): TaskEntity?

    @Query("SELECT * FROM tasks WHERE user_id = :userId AND status != 'COMPLETED' ORDER BY priority DESC, deadline ASC")
    fun getPendingTasks(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE user_id = :userId AND status = 'COMPLETED' ORDER BY completed_at DESC")
    fun getCompletedTasks(userId: String): Flow<List<TaskEntity>>

    @Query(
        "SELECT COUNT(*) FROM tasks WHERE user_id = :userId AND status = 'COMPLETED' AND completed_at BETWEEN :start AND :end",
    )
    fun getCompletedCountBetween(
        start: Long,
        end: Long,
        userId: String,
    ): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE user_id = :userId AND status != 'COMPLETED'")
    fun getPendingCount(userId: String): Flow<Int>

    @Query(
        """
        SELECT * FROM tasks
        WHERE user_id = :userId
          AND (title LIKE :query OR description LIKE :query)
        ORDER BY created_at DESC
        LIMIT :limit
        """,
    )
    suspend fun search(
        userId: String,
        query: String,
        limit: Int = 30,
    ): List<TaskEntity>

    @Query("SELECT id FROM tasks WHERE user_id = :userId AND status != 'COMPLETED' AND deadline IS NOT NULL")
    suspend fun getScheduledReminderIds(userId: String): List<Long>

    @Query("DELETE FROM tasks WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("UPDATE tasks SET user_id = :userId WHERE user_id = ''")
    suspend fun claimUnownedRecords(userId: String)
}
