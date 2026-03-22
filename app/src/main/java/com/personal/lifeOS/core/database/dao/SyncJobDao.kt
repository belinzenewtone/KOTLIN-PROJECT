@file:Suppress("MaxLineLength")

package com.personal.lifeOS.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personal.lifeOS.core.database.entity.SyncJobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncJobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(job: SyncJobEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(jobs: List<SyncJobEntity>)

    @Update
    suspend fun update(job: SyncJobEntity)

    @Query(
        "SELECT * FROM sync_jobs " +
            "WHERE job_type = :jobType AND entity_type = :entityType AND entity_id = :entityId " +
            "AND status IN ('QUEUED', 'FAILED', 'SYNCING') " +
            "ORDER BY updated_at DESC LIMIT 1",
    )
    suspend fun findActiveJob(
        jobType: String,
        entityType: String,
        entityId: String,
    ): SyncJobEntity?

    @Query(
        "SELECT * FROM sync_jobs " +
            "WHERE status IN ('QUEUED', 'FAILED') AND next_run_at <= :now " +
            "ORDER BY next_run_at ASC LIMIT :limit",
    )
    suspend fun getDueJobs(
        now: Long = System.currentTimeMillis(),
        limit: Int = 50,
    ): List<SyncJobEntity>

    @Query("SELECT * FROM sync_jobs ORDER BY created_at DESC")
    fun observeAll(): Flow<List<SyncJobEntity>>

    @Query("DELETE FROM sync_jobs WHERE status = 'SYNCED' AND updated_at < :olderThan")
    suspend fun pruneSynced(olderThan: Long): Int

    @Query("SELECT COUNT(*) FROM sync_jobs WHERE status IN ('QUEUED', 'FAILED')")
    suspend fun getPendingCount(): Int
}
