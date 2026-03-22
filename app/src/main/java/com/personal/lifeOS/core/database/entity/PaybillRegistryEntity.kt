package com.personal.lifeOS.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Paybill registry — learns every paybill account the user has paid to.
 * Populated automatically during M-Pesa import for PAYBILL transactions.
 * Powers smart recurring suggestions and quick-fill on manual entry.
 */
@Entity(
    tableName = "paybill_registry",
    primaryKeys = ["user_id", "paybill_number"],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["usage_count"]),
    ],
)
data class PaybillRegistryEntity(
    @ColumnInfo(name = "paybill_number")
    val paybillNumber: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "last_seen_at")
    val lastSeenAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "usage_count")
    val usageCount: Int = 1,
    @ColumnInfo(name = "last_amount_kes")
    val lastAmountKes: Double = 0.0,
    @ColumnInfo(name = "user_id")
    val userId: String = "",
)
