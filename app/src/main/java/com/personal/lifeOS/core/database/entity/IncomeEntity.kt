package com.personal.lifeOS.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "incomes",
    primaryKeys = ["user_id", "id"],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["date"]),
        Index(value = ["source"]),
    ],
)
data class IncomeEntity(
    val id: Long = 0L,
    val amount: Double,
    val source: String,
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    @ColumnInfo(name = "is_recurring")
    val isRecurring: Boolean = false,
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
