@file:Suppress("MaxLineLength")

package com.personal.lifeOS.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personal.lifeOS.core.database.entity.AssistantConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantConversationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: AssistantConversationEntity): Long

    @Update
    suspend fun update(conversation: AssistantConversationEntity)

    @Query(
        "SELECT * FROM assistant_conversations WHERE user_id = :userId AND deleted_at IS NULL ORDER BY updated_at DESC",
    )
    fun observeForUser(userId: String): Flow<List<AssistantConversationEntity>>

    @Query(
        "SELECT * FROM assistant_conversations " +
            "WHERE user_id = :userId AND deleted_at IS NULL " +
            "ORDER BY updated_at DESC LIMIT 1",
    )
    suspend fun getLatestForUser(userId: String): AssistantConversationEntity?

    @Query("SELECT * FROM assistant_conversations WHERE user_id = :userId AND id = :id LIMIT 1")
    suspend fun getById(
        userId: String,
        id: Long,
    ): AssistantConversationEntity?

    @Query("DELETE FROM assistant_conversations WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)
}
