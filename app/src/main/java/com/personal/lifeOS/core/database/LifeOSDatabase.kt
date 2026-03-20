package com.personal.lifeOS.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.personal.lifeOS.core.database.converter.DateConverters
import com.personal.lifeOS.core.database.dao.AppUpdateInfoDao
import com.personal.lifeOS.core.database.dao.AssistantConversationDao
import com.personal.lifeOS.core.database.dao.AssistantMessageDao
import com.personal.lifeOS.core.database.dao.BudgetDao
import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.dao.ExportHistoryDao
import com.personal.lifeOS.core.database.dao.ImportAuditDao
import com.personal.lifeOS.core.database.dao.IncomeDao
import com.personal.lifeOS.core.database.dao.InsightCardDao
import com.personal.lifeOS.core.database.dao.MerchantCategoryDao
import com.personal.lifeOS.core.database.dao.RecurringRuleDao
import com.personal.lifeOS.core.database.dao.ReviewSnapshotDao
import com.personal.lifeOS.core.database.dao.SyncJobDao
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.database.entity.AppUpdateInfoEntity
import com.personal.lifeOS.core.database.entity.AssistantConversationEntity
import com.personal.lifeOS.core.database.entity.AssistantMessageEntity
import com.personal.lifeOS.core.database.entity.BudgetEntity
import com.personal.lifeOS.core.database.entity.EventEntity
import com.personal.lifeOS.core.database.entity.ExportHistoryEntity
import com.personal.lifeOS.core.database.entity.ImportAuditEntity
import com.personal.lifeOS.core.database.entity.IncomeEntity
import com.personal.lifeOS.core.database.entity.InsightCardEntity
import com.personal.lifeOS.core.database.entity.MerchantCategoryEntity
import com.personal.lifeOS.core.database.entity.RecurringRuleEntity
import com.personal.lifeOS.core.database.entity.ReviewSnapshotEntity
import com.personal.lifeOS.core.database.entity.SyncJobEntity
import com.personal.lifeOS.core.database.entity.TaskEntity
import com.personal.lifeOS.core.database.entity.TransactionEntity
import com.personal.lifeOS.core.database.entity.UserEntity

@Database(
    entities = [
        TransactionEntity::class,
        MerchantCategoryEntity::class,
        TaskEntity::class,
        EventEntity::class,
        BudgetEntity::class,
        IncomeEntity::class,
        RecurringRuleEntity::class,
        UserEntity::class,
        SyncJobEntity::class,
        ImportAuditEntity::class,
        AssistantConversationEntity::class,
        AssistantMessageEntity::class,
        InsightCardEntity::class,
        ReviewSnapshotEntity::class,
        AppUpdateInfoEntity::class,
        ExportHistoryEntity::class,
    ],
    version = 9,
    exportSchema = true,
)
@TypeConverters(DateConverters::class)
@Suppress("TooManyFunctions")
abstract class LifeOSDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    abstract fun merchantCategoryDao(): MerchantCategoryDao

    abstract fun taskDao(): TaskDao

    abstract fun eventDao(): EventDao

    abstract fun budgetDao(): BudgetDao

    abstract fun incomeDao(): IncomeDao

    abstract fun recurringRuleDao(): RecurringRuleDao

    abstract fun syncJobDao(): SyncJobDao

    abstract fun importAuditDao(): ImportAuditDao

    abstract fun assistantConversationDao(): AssistantConversationDao

    abstract fun assistantMessageDao(): AssistantMessageDao

    abstract fun insightCardDao(): InsightCardDao

    abstract fun reviewSnapshotDao(): ReviewSnapshotDao

    abstract fun appUpdateInfoDao(): AppUpdateInfoDao

    abstract fun exportHistoryDao(): ExportHistoryDao
}
