package com.personal.lifeOS.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personal.lifeOS.core.database.entity.ReviewSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: ReviewSnapshotEntity): Long

    @Query("SELECT * FROM review_snapshots WHERE user_id = :userId AND deleted_at IS NULL ORDER BY period_start DESC")
    fun observeAll(userId: String): Flow<List<ReviewSnapshotEntity>>
}
