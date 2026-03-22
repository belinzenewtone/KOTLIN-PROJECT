package com.personal.lifeOS.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "import_audit",
    primaryKeys = ["user_id", "id"],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["outcome"]),
        Index(value = ["imported_at"]),
        Index(value = ["mpesa_code"]),
    ],
)
data class ImportAuditEntity(
    val id: Long = 0L,
    @ColumnInfo(name = "user_id")
    val userId: String = "",
    @ColumnInfo(name = "raw_message")
    val rawMessage: String = "",
    @ColumnInfo(name = "mpesa_code")
    val mpesaCode: String? = null,
    val amount: Double? = null,
    val merchant: String? = null,
    val outcome: String,
    @ColumnInfo(name = "failure_reason")
    val failureReason: String? = null,
    @ColumnInfo(name = "imported_at")
    val importedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    /** Numeric confidence score from the parser: 1.0=HIGH, 0.5=MEDIUM, 0.0=LOW/UNKNOWN */
    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Double = 0.0,
)
