package com.personal.lifeOS.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personal.lifeOS.core.database.entity.AppUpdateInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUpdateInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(info: AppUpdateInfoEntity): Long

    @Query("SELECT * FROM app_update_info WHERE user_id = :userId ORDER BY checked_at DESC LIMIT 1")
    fun observeLatest(userId: String): Flow<AppUpdateInfoEntity?>
}
