package com.personal.lifeOS.core.di

import android.content.Context
import androidx.room.Room
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

        return Room.databaseBuilder(
            context,
            LifeOSDatabase::class.java,
            "beltech_db",
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
            )
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
