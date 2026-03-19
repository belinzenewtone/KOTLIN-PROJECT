@file:Suppress("MaxLineLength")

package com.personal.lifeOS.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personal.lifeOS.core.database.entity.ImportAuditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportAuditDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(audit: ImportAuditEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(audits: List<ImportAuditEntity>)

    @Query("SELECT * FROM import_audit WHERE user_id = :userId ORDER BY imported_at DESC")
    fun observeByUser(userId: String): Flow<List<ImportAuditEntity>>

    @Query(
        "SELECT * FROM import_audit " +
            "WHERE user_id = :userId AND outcome = :outcome " +
            "ORDER BY imported_at DESC LIMIT :limit",
    )
    suspend fun findRecentByOutcome(
        userId: String,
        outcome: String,
        limit: Int = 100,
    ): List<ImportAuditEntity>
}
