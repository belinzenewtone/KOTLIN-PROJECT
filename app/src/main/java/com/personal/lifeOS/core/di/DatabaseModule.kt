package com.personal.lifeOS.core.di

import android.content.Context
import androidx.room.Room
import com.personal.lifeOS.core.database.LifeOSDatabase
import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.dao.MerchantCategoryDao
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LifeOSDatabase {
        // Clean up old encrypted database if it exists
        val oldDb = context.getDatabasePath("lifeos_db")
        if (oldDb.exists()) {
            context.deleteDatabase("lifeos_db")
        }

        return Room.databaseBuilder(
            context,
            LifeOSDatabase::class.java,
            "beltech_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTransactionDao(db: LifeOSDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideMerchantCategoryDao(db: LifeOSDatabase): MerchantCategoryDao = db.merchantCategoryDao()

    @Provides
    fun provideTaskDao(db: LifeOSDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideEventDao(db: LifeOSDatabase): EventDao = db.eventDao()
}
