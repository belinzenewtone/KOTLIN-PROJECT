package com.personal.lifeOS.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personal.lifeOS.core.database.entity.MerchantCategoryEntity

@Dao
interface MerchantCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(merchantCategory: MerchantCategoryEntity)

    @Query("SELECT * FROM merchant_categories WHERE merchant = :merchant AND user_id = :userId LIMIT 1")
    suspend fun getByMerchant(
        merchant: String,
        userId: String,
    ): MerchantCategoryEntity?

    @Query("SELECT * FROM merchant_categories WHERE user_id = :userId ORDER BY merchant ASC")
    suspend fun getAll(userId: String): List<MerchantCategoryEntity>

    @Query("SELECT * FROM merchant_categories WHERE user_id = :userId OR user_id = '' ORDER BY merchant ASC")
    suspend fun getAllForSync(userId: String): List<MerchantCategoryEntity>

    @Query(
        "UPDATE merchant_categories SET category = :category, userCorrected = 1, confidence = 1.0 WHERE merchant = :merchant AND user_id = :userId",
    )
    suspend fun updateCategory(
        merchant: String,
        category: String,
        userId: String,
    )

    @Query("DELETE FROM merchant_categories WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("UPDATE merchant_categories SET user_id = :userId WHERE user_id = ''")
    suspend fun claimUnownedRecords(userId: String)
}
