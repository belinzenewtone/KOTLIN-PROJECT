package com.personal.lifeOS.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "budgets",
    primaryKeys = ["user_id", "id"],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["category"]),
        Index(value = ["period"]),
    ],
)
data class BudgetEntity(
    val id: Long = 0L,
    val category: String,
    @ColumnInfo(name = "limit_amount")
    val limitAmount: Double,
    val period: String = "MONTHLY",
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
