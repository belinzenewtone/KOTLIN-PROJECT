package com.personal.lifeOS.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personal.lifeOS.core.database.entity.PaybillRegistryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaybillRegistryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entry: PaybillRegistryEntity)

    @Query("""
        UPDATE paybill_registry
        SET usage_count = usage_count + 1,
            last_seen_at = :seenAt,
            last_amount_kes = :amount,
            display_name = :displayName
        WHERE user_id = :userId AND paybill_number = :paybillNumber
    """)
    suspend fun increment(
        userId: String,
        paybillNumber: String,
        displayName: String,
        seenAt: Long,
        amount: Double,
    )

    @Query("SELECT * FROM paybill_registry WHERE user_id = :userId ORDER BY usage_count DESC")
    fun observeAll(userId: String): Flow<List<PaybillRegistryEntity>>

    @Query("SELECT * FROM paybill_registry WHERE user_id = :userId AND paybill_number = :paybillNumber LIMIT 1")
    suspend fun getByNumber(userId: String, paybillNumber: String): PaybillRegistryEntity?

    @Query("""
        SELECT * FROM paybill_registry
        WHERE user_id = :userId AND usage_count >= :minCount
        ORDER BY last_seen_at DESC
    """)
    suspend fun getFrequent(userId: String, minCount: Int = 3): List<PaybillRegistryEntity>
}
