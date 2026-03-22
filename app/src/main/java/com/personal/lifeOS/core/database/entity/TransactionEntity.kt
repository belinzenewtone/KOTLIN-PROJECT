package com.personal.lifeOS.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "transactions",
    primaryKeys = ["user_id", "id"],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["date"]),
        Index(value = ["category"]),
        Index(value = ["merchant"]),
        Index(value = ["mpesa_code"]),
        Index(value = ["source_hash"]),
        Index(value = ["semantic_hash"]),
    ],
)
data class TransactionEntity(
    val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val category: String,
    val date: Long, // epoch millis
    val source: String = "MPESA", // MPESA, Manual, Bank
    @ColumnInfo(name = "transaction_type")
    val transactionType: String = "SENT", // SENT, RECEIVED, PAID, WITHDRAWN
    @ColumnInfo(name = "mpesa_code")
    val mpesaCode: String? = null,
    @ColumnInfo(name = "source_hash")
    val sourceHash: String? = null,
    @ColumnInfo(name = "raw_sms")
    val rawSms: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sync_state")
    val syncState: String = "LOCAL_ONLY",
    @ColumnInfo(name = "record_source")
    val recordSource: String = "SMS",
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
    val revision: Long = 0L,
    @ColumnInfo(name = "user_id")
    val userId: String = "",
    /** Category inferred by CategoryInferenceEngine (may differ from user-assigned category). */
    @ColumnInfo(name = "inferred_category")
    val inferredCategory: String? = null,
    /** Source that produced the inferred category: MPESA_KIND | KEYWORD | AMOUNT_HEURISTIC */
    @ColumnInfo(name = "inference_source")
    val inferenceSource: String? = null,
    /** Semantic hash for cross-device dedup: SHA-256("$transactionType|${"%.2f".format(amount)}|$yyyyMMdd|${merchant.lowercase()}") */
    @ColumnInfo(name = "semantic_hash")
    val semanticHash: String? = null,
)
