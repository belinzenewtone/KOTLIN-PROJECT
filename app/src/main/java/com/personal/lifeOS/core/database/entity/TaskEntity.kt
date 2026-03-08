package com.personal.lifeOS.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "tasks",
    primaryKeys = ["user_id", "id"],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["deadline"]),
        Index(value = ["status"]),
        Index(value = ["priority"]),
    ],
)
data class TaskEntity(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH, CRITICAL
    val deadline: Long? = null, // epoch millis
    val status: String = "PENDING", // PENDING, IN_PROGRESS, COMPLETED
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "user_id")
    val userId: String = "",
)
