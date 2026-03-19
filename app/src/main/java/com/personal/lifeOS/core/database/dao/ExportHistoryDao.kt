package com.personal.lifeOS.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personal.lifeOS.core.database.entity.ExportHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExportHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ExportHistoryEntity): Long

    @Query(
        """
        SELECT * FROM export_history
        WHERE user_id = :userId
        ORDER BY exported_at DESC
        LIMIT :limit
        """,
    )
    fun observeRecent(
        userId: String,
        limit: Int = 20,
    ): Flow<List<ExportHistoryEntity>>
}
