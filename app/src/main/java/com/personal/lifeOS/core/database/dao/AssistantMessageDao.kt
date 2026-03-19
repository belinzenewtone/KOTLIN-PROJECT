@file:Suppress("MaxLineLength")

package com.personal.lifeOS.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personal.lifeOS.core.database.entity.AssistantMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: AssistantMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<AssistantMessageEntity>)

    @Query(
        "SELECT * FROM assistant_messages " +
            "WHERE user_id = :userId AND conversation_id = :conversationId " +
            "AND deleted_at IS NULL ORDER BY created_at ASC",
    )
    fun observeConversation(
        userId: String,
        conversationId: Long,
    ): Flow<List<AssistantMessageEntity>>

    @Query(
        "SELECT * FROM assistant_messages " +
            "WHERE user_id = :userId AND conversation_id = :conversationId " +
            "AND deleted_at IS NULL ORDER BY created_at ASC",
    )
    suspend fun getConversationMessages(
        userId: String,
        conversationId: Long,
    ): List<AssistantMessageEntity>
}
