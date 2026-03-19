package com.personal.lifeOS.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "insight_cards",
    primaryKeys = ["user_id", "id"],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["kind"]),
        Index(value = ["created_at"]),
        Index(value = ["fresh_until"]),
    ],
)
data class InsightCardEntity(
    val id: Long = 0L,
    @ColumnInfo(name = "user_id")
    val userId: String = "",
    val kind: String,
    val title: String,
    val body: String,
    val confidence: Double? = null,
    @ColumnInfo(name = "is_ai_generated")
    val isAiGenerated: Boolean = false,
    @ColumnInfo(name = "fresh_until")
    val freshUntil: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sync_state")
    val syncState: String = "LOCAL_ONLY",
    @ColumnInfo(name = "record_source")
    val recordSource: String = "SYSTEM",
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
    val revision: Long = 0L,
)
