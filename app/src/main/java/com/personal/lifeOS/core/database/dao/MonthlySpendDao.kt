package com.personal.lifeOS.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.personal.lifeOS.core.database.entity.MonthlySpendView
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlySpendDao {
    /**
     * Returns the most recent [limit] months of aggregated spending for a user,
     * newest-first. Use for month-over-month trend charts.
     */
    @Query(
        """
        SELECT * FROM monthly_spend
        WHERE user_id = :userId
        ORDER BY spend_month DESC
        LIMIT :limit
        """,
    )
    fun getMonthlySpend(
        userId: String,
        limit: Int = 12,
    ): Flow<List<MonthlySpendView>>

    /**
     * Returns the aggregated spend row for a specific month.
     * [month] format: "YYYY-MM"
     */
    @Query(
        """
        SELECT * FROM monthly_spend
        WHERE user_id = :userId AND spend_month = :month
        LIMIT 1
        """,
    )
    fun getSpendForMonth(
        userId: String,
        month: String,
    ): Flow<MonthlySpendView?>
}
