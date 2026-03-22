package com.personal.lifeOS.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.personal.lifeOS.core.database.entity.DailySpendView
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySpendDao {
    /**
     * Returns rows from the daily_spend view for a specific user within a date range.
     * [startDate] and [endDate] must be "YYYY-MM-DD" strings (inclusive on both ends).
     */
    @Query(
        """
        SELECT * FROM daily_spend
        WHERE user_id = :userId
          AND spend_date BETWEEN :startDate AND :endDate
        ORDER BY spend_date ASC
        """,
    )
    fun getDailySpend(
        userId: String,
        startDate: String,
        endDate: String,
    ): Flow<List<DailySpendView>>
}
