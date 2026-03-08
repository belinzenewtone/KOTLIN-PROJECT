package com.personal.lifeOS.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "merchant_categories",
    primaryKeys = ["user_id", "id"],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["user_id", "merchant"], unique = true),
    ],
)
data class MerchantCategoryEntity(
    val id: Long = 0,
    val merchant: String,
    val category: String,
    val confidence: Float = 1.0f, // 0.0 to 1.0
    val userCorrected: Boolean = false,
    @ColumnInfo(name = "user_id")
    val userId: String = "",
)
