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
    @ColumnInfo(name = "raw_sms")
    val rawSms: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "user_id")
    val userId: String = "",
)
