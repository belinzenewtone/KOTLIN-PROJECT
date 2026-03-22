package com.personal.lifeOS.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Tracks individual Fuliza loan lifecycle events (draws and repayments).
 *
 * Each draw creates a new row with status = OPEN. Every subsequent repayment
 * that matches the same [drawCode] increments [totalRepaidKes] and updates
 * [status] to PARTIALLY_REPAID or CLOSED once fully repaid.
 *
 * This enables the Finance screen to display: "Outstanding Fuliza: Ksh X"
 * — a feature unique to this app vs PMA and DART.
 */
@Entity(
    tableName = "fuliza_loans",
    primaryKeys = ["user_id", "id"],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["user_id", "draw_code"], unique = true),
        Index(value = ["status"]),
    ],
)
data class FulizaLoanEntity(
    val id: Long = 0,
    @ColumnInfo(name = "draw_code")
    val drawCode: String,
    @ColumnInfo(name = "draw_amount_kes")
    val drawAmountKes: Double,
    @ColumnInfo(name = "total_repaid_kes")
    val totalRepaidKes: Double = 0.0,
    /** OPEN | PARTIALLY_REPAID | CLOSED */
    val status: String = FulizaLoanStatus.OPEN.name,
    @ColumnInfo(name = "draw_date")
    val drawDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_repayment_date")
    val lastRepaymentDate: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "user_id")
    val userId: String = "",
) {
    val outstandingKes: Double get() = (drawAmountKes - totalRepaidKes).coerceAtLeast(0.0)
}

enum class FulizaLoanStatus { OPEN, PARTIALLY_REPAID, CLOSED }
