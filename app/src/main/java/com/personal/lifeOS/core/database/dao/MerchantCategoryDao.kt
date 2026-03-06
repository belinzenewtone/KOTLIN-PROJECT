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

    @Query("SELECT * FROM merchant_categories WHERE merchant = :merchant LIMIT 1")
    suspend fun getByMerchant(merchant: String): MerchantCategoryEntity?

    @Query("SELECT * FROM merchant_categories ORDER BY merchant ASC")
    suspend fun getAll(): List<MerchantCategoryEntity>

    @Query("UPDATE merchant_categories SET category = :category, userCorrected = 1, confidence = 1.0 WHERE merchant = :merchant")
    suspend fun updateCategory(merchant: String, category: String)
}
