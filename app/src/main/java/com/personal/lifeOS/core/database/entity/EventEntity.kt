package com.personal.lifeOS.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "events",
    primaryKeys = ["user_id", "id"],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["date"]),
        Index(value = ["type"]),
        Index(value = ["status"]),
    ],
)
data class EventEntity(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val date: Long,
    @ColumnInfo(name = "end_date")
    val endDate: Long? = null,
    val type: String = "PERSONAL",
    val importance: String = "NEUTRAL",
    val status: String = "PENDING",
    @ColumnInfo(name = "has_reminder")
    val hasReminder: Boolean = false,
    @ColumnInfo(name = "reminder_minutes_before")
    val reminderMinutesBefore: Int = 15,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sync_state")
    val syncState: String = "LOCAL_ONLY",
    @ColumnInfo(name = "record_source")
    val recordSource: String = "MANUAL",
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
    val revision: Long = 0L,
    @ColumnInfo(name = "user_id")
    val userId: String = "",
)
