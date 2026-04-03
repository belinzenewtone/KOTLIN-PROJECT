package com.personal.lifeOS.core.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.personal.lifeOS.core.database.DatabaseMigrations
import com.personal.lifeOS.core.database.LifeOSDatabase
import com.personal.lifeOS.core.database.dao.AppUpdateInfoDao
import com.personal.lifeOS.core.database.dao.AssistantConversationDao
import com.personal.lifeOS.core.database.dao.AssistantMessageDao
import com.personal.lifeOS.core.database.dao.BudgetDao
import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.dao.ExportHistoryDao
import com.personal.lifeOS.core.database.dao.FulizaLoanDao
import com.personal.lifeOS.core.database.dao.ImportAuditDao
import com.personal.lifeOS.core.database.dao.IncomeDao
import com.personal.lifeOS.core.database.dao.InsightCardDao
import com.personal.lifeOS.core.database.dao.MerchantCategoryDao
import com.personal.lifeOS.core.database.dao.PaybillRegistryDao
import com.personal.lifeOS.core.database.dao.RecurringRuleDao
import com.personal.lifeOS.core.database.dao.DailySpendDao
import com.personal.lifeOS.core.database.dao.MonthlySpendDao
import com.personal.lifeOS.core.database.dao.ReviewSnapshotDao
import com.personal.lifeOS.core.database.dao.SyncJobDao
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.observability.AppTelemetry
import com.personal.lifeOS.core.security.DatabaseEncryptionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("TooManyFunctions")
object DatabaseModule {
    private const val DATABASE_NAME = "beltech_db"

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): LifeOSDatabase {
        // Clean up old encrypted database if it exists
        val oldDb = context.getDatabasePath("lifeos_db")
        if (oldDb.exists()) {
            context.deleteDatabase("lifeos_db")
        }

        val useEncryptedDb =
            DatabaseEncryptionManager.shouldUseEncryption(
                context = context,
                dbName = DATABASE_NAME,
            )
        val encryptedFactory =
            if (useEncryptedDb) {
                runCatching { DatabaseEncryptionManager.createSupportFactory(context) }
                    .onFailure { error ->
                        AppTelemetry.captureError(
                            throwable = error,
                            context = mapOf("event" to "db_encrypted_factory_create", "db_name" to DATABASE_NAME),
                        )
                    }.getOrNull()
            } else {
                null
            }

        if (encryptedFactory == null) {
            AppTelemetry.trackEvent(
                name = "db_encryption_not_applied",
                attributes = mapOf("db_name" to DATABASE_NAME, "reason" to "legacy_plaintext_install"),
            )
        }

        val preferredDatabase = buildDatabase(context, openHelperFactory = encryptedFactory)
        if (canOpenDatabase(preferredDatabase)) {
            return preferredDatabase
        }
        preferredDatabase.close()

        // Safety fallback 1: switch mode (encrypted <-> plaintext) and retry.
        val fallbackFactory =
            if (encryptedFactory != null) {
                null
            } else {
                runCatching { DatabaseEncryptionManager.createSupportFactory(context) }.getOrNull()
            }
        val fallbackDatabase = buildDatabase(context, openHelperFactory = fallbackFactory)
        if (canOpenDatabase(fallbackDatabase)) {
            AppTelemetry.trackEvent(
                name = "db_mode_fallback_applied",
                attributes = mapOf("db_name" to DATABASE_NAME, "mode" to if (fallbackFactory == null) "plaintext" else "encrypted"),
                captureAsMessage = true,
            )
            return fallbackDatabase
        }
        fallbackDatabase.close()

        // Safety fallback 2: reset corrupted local DB so app can boot.
        AppTelemetry.trackEvent(
            name = "db_reset_after_open_failure",
            attributes = mapOf("db_name" to DATABASE_NAME),
            captureAsMessage = true,
        )
        context.deleteDatabase(DATABASE_NAME)
        val resetDatabase = buildDatabase(context, openHelperFactory = null)
        canOpenDatabase(resetDatabase)
        return resetDatabase
    }

    private fun buildDatabase(
        context: Context,
        openHelperFactory: SupportSQLiteOpenHelper.Factory?,
    ): LifeOSDatabase {
        val builder =
            Room.databaseBuilder(
                context,
                LifeOSDatabase::class.java,
                DATABASE_NAME,
            )
                .addMigrations(
                    DatabaseMigrations.MIGRATION_1_2,
                    DatabaseMigrations.MIGRATION_2_3,
                    DatabaseMigrations.MIGRATION_3_4,
                    DatabaseMigrations.MIGRATION_4_5,
                    DatabaseMigrations.MIGRATION_5_6,
                    DatabaseMigrations.MIGRATION_6_7,
                    DatabaseMigrations.MIGRATION_7_8,
                    DatabaseMigrations.MIGRATION_8_9,
                    DatabaseMigrations.MIGRATION_9_10,
                    DatabaseMigrations.MIGRATION_10_11,
                    DatabaseMigrations.MIGRATION_11_12,
                    DatabaseMigrations.MIGRATION_12_13,
                    DatabaseMigrations.MIGRATION_13_14,
                )
        if (openHelperFactory != null) {
            builder.openHelperFactory(openHelperFactory)
        }
        return builder.build()
    }

    private fun canOpenDatabase(db: LifeOSDatabase): Boolean {
        return runCatching {
            db.openHelper.writableDatabase
            true
        }.onFailure { error ->
            AppTelemetry.captureError(
                throwable = error,
                context = mapOf("event" to "db_open_failed", "db_name" to DATABASE_NAME),
            )
        }.getOrDefault(false)
    }

    @Provides
    fun provideTransactionDao(db: LifeOSDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideMerchantCategoryDao(db: LifeOSDatabase): MerchantCategoryDao = db.merchantCategoryDao()

    @Provides
    fun provideTaskDao(db: LifeOSDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideEventDao(db: LifeOSDatabase): EventDao = db.eventDao()

    @Provides
    fun provideBudgetDao(db: LifeOSDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideIncomeDao(db: LifeOSDatabase): IncomeDao = db.incomeDao()

    @Provides
    fun provideRecurringRuleDao(db: LifeOSDatabase): RecurringRuleDao = db.recurringRuleDao()

    @Provides
    fun provideSyncJobDao(db: LifeOSDatabase): SyncJobDao = db.syncJobDao()

    @Provides
    fun provideImportAuditDao(db: LifeOSDatabase): ImportAuditDao = db.importAuditDao()

    @Provides
    fun provideAssistantConversationDao(db: LifeOSDatabase): AssistantConversationDao = db.assistantConversationDao()

    @Provides
    fun provideAssistantMessageDao(db: LifeOSDatabase): AssistantMessageDao = db.assistantMessageDao()

    @Provides
    fun provideInsightCardDao(db: LifeOSDatabase): InsightCardDao = db.insightCardDao()

    @Provides
    fun provideReviewSnapshotDao(db: LifeOSDatabase): ReviewSnapshotDao = db.reviewSnapshotDao()

    @Provides
    fun provideAppUpdateInfoDao(db: LifeOSDatabase): AppUpdateInfoDao = db.appUpdateInfoDao()

    @Provides
    fun provideExportHistoryDao(db: LifeOSDatabase): ExportHistoryDao = db.exportHistoryDao()

    @Provides
    fun provideFulizaLoanDao(db: LifeOSDatabase): FulizaLoanDao = db.fulizaLoanDao()

    @Provides
    fun providePaybillRegistryDao(db: LifeOSDatabase): PaybillRegistryDao = db.paybillRegistryDao()

    @Provides
    fun provideDailySpendDao(db: LifeOSDatabase): DailySpendDao = db.dailySpendDao()

    @Provides
    fun provideMonthlySpendDao(db: LifeOSDatabase): MonthlySpendDao = db.monthlySpendDao()
}
