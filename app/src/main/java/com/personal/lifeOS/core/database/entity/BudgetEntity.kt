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
    @ColumnInfo(name = "user_id")
    val userId: String = "",
)
