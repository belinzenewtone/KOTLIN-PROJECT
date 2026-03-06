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
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LifeOSDatabase {
        // SQLCipher encrypted database
        val passphrase = getOrCreatePassphrase(context)
        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(
            context,
            LifeOSDatabase::class.java,
            "lifeos_db"
        )
            .openHelperFactory(factory)
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

    /**
     * Generate or retrieve a stored encryption passphrase.
     * In production, use Android Keystore for key management.
     */
    private fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences("lifeos_security", Context.MODE_PRIVATE)
        val existing = prefs.getString("db_key", null)
        if (existing != null) {
            return existing.toByteArray()
        }
        val generated = java.util.UUID.randomUUID().toString()
        prefs.edit().putString("db_key", generated).apply()
        return generated.toByteArray()
    }
}
