package com.personal.lifeOS.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "events",
    indices = [
        Index(value = ["date"]),
        Index(value = ["type"])
    ]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    val description: String = "",

    val date: Long, // epoch millis

    @ColumnInfo(name = "end_date")
    val endDate: Long? = null,

    val type: String = "PERSONAL",

    val importance: String = "NEUTRAL", // NEUTRAL, IMPORTANT, URGENT

    @ColumnInfo(name = "has_reminder")
    val hasReminder: Boolean = false,

    @ColumnInfo(name = "reminder_minutes_before")
    val reminderMinutesBefore: Int = 15,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
