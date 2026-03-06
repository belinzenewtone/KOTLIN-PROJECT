package com.personal.lifeOS.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "merchant_categories",
    indices = [Index(value = ["merchant"], unique = true)]
)
data class MerchantCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val merchant: String,

    val category: String,

    val confidence: Float = 1.0f, // 0.0 to 1.0

    val userCorrected: Boolean = false
)
