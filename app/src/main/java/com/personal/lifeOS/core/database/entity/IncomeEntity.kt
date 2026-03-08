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
    @ColumnInfo(name = "user_id")
    val userId: String = "",
)
