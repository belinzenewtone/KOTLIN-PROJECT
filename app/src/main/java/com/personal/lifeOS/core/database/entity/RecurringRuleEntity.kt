package com.personal.lifeOS.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "recurring_rules",
    primaryKeys = ["user_id", "id"],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["next_run_at"]),
        Index(value = ["enabled"]),
    ],
)
data class RecurringRuleEntity(
    val id: Long = 0L,
    val title: String,
    val type: String,
    val cadence: String,
    @ColumnInfo(name = "next_run_at")
    val nextRunAt: Long,
    val amount: Double? = null,
    val enabled: Boolean = true,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "user_id")
    val userId: String = "",
)
