@file:Suppress("MaxLineLength")

package com.personal.lifeOS.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.DatabaseView

/**
 * Pre-aggregated monthly spending view — powers month-over-month trend charts
 * without full table scans.
 *
 * spend_month format: "YYYY-MM"
 */
@DatabaseView(
    viewName = "monthly_spend",
    // Single-line SQL avoids Room's literal-string whitespace comparison failures.
    // Any change here MUST be reflected in MIGRATION_14_15 with a matching single-line execSQL.
    value = "SELECT user_id, strftime('%Y-%m', date / 1000, 'unixepoch', 'localtime') AS spend_month, SUM(amount) AS total_amount, COUNT(*) AS tx_count FROM transactions WHERE deleted_at IS NULL AND UPPER(transaction_type) IN ('SENT', 'AIRTIME', 'PAYBILL', 'BUY_GOODS', 'WITHDRAW', 'PAID', 'WITHDRAWN') GROUP BY user_id, strftime('%Y-%m', date / 1000, 'unixepoch', 'localtime')",
)
data class MonthlySpendView(
    @ColumnInfo(name = "user_id") val userId: String,
    /** Month string in "YYYY-MM" format (local timezone). */
    @ColumnInfo(name = "spend_month") val spendMonth: String,
    @ColumnInfo(name = "total_amount") val totalAmount: Double,
    @ColumnInfo(name = "tx_count") val txCount: Int,
)
