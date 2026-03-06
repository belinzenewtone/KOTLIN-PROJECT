package com.personal.lifeOS.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.personal.lifeOS.core.database.converter.DateConverters
import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.dao.MerchantCategoryDao
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.database.entity.EventEntity
import com.personal.lifeOS.core.database.entity.MerchantCategoryEntity
import com.personal.lifeOS.core.database.entity.TaskEntity
import com.personal.lifeOS.core.database.entity.TransactionEntity
import com.personal.lifeOS.core.database.entity.UserEntity

@Database(
    entities = [
        TransactionEntity::class,
        MerchantCategoryEntity::class,
        TaskEntity::class,
        EventEntity::class,
        UserEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(DateConverters::class)
abstract class LifeOSDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun merchantCategoryDao(): MerchantCategoryDao
    abstract fun taskDao(): TaskDao
    abstract fun eventDao(): EventDao
}
