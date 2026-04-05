@file:Suppress("MaxLineLength")

package com.personal.lifeOS.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.DatabaseView

/**
 * Pre-aggregated daily spending view — avoids loading all transaction rows into memory
 * when building per-day charts. Room creates/recreates this view automatically.
 *
 * spend_date format: "YYYY-MM-DD" (SQLite strftime with local timezone via unixepoch).
 */
@DatabaseView(
    viewName = "daily_spend",
    // Single-line SQL avoids Room's literal-string whitespace comparison failures.
    // Any change here MUST be reflected in MIGRATION_14_15 with a matching single-line execSQL.
    value = "SELECT user_id, strftime('%Y-%m-%d', date / 1000, 'unixepoch', 'localtime') AS spend_date, SUM(amount) AS total_amount, COUNT(*) AS tx_count FROM transactions WHERE deleted_at IS NULL AND UPPER(transaction_type) IN ('SENT', 'AIRTIME', 'PAYBILL', 'BUY_GOODS', 'WITHDRAW', 'PAID', 'WITHDRAWN') GROUP BY user_id, strftime('%Y-%m-%d', date / 1000, 'unixepoch', 'localtime')",
)
data class DailySpendView(
    @ColumnInfo(name = "user_id") val userId: String,
    /** Date string in "YYYY-MM-DD" format (local timezone). */
    @ColumnInfo(name = "spend_date") val spendDate: String,
    @ColumnInfo(name = "total_amount") val totalAmount: Double,
    @ColumnInfo(name = "tx_count") val txCount: Int,
)
