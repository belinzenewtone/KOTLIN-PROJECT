package com.personal.lifeOS.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "assistant_messages",
    primaryKeys = ["user_id", "id"],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["conversation_id"]),
        Index(value = ["created_at"]),
    ],
)
data class AssistantMessageEntity(
    val id: Long = 0L,
    @ColumnInfo(name = "user_id")
    val userId: String = "",
    @ColumnInfo(name = "conversation_id")
    val conversationId: Long,
    val role: String,
    val content: String,
    @ColumnInfo(name = "action_payload")
    val actionPayload: String? = null,
    @ColumnInfo(name = "is_preview")
    val isPreview: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sync_state")
    val syncState: String = "LOCAL_ONLY",
    @ColumnInfo(name = "record_source")
    val recordSource: String = "ASSISTANT",
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
    val revision: Long = 0L,
)
