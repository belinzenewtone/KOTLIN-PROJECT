package com.personal.lifeOS.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.personal.lifeOS.core.database.converter.DateConverters
import com.personal.lifeOS.core.database.dao.BudgetDao
import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.dao.IncomeDao
import com.personal.lifeOS.core.database.dao.MerchantCategoryDao
import com.personal.lifeOS.core.database.dao.RecurringRuleDao
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.database.entity.BudgetEntity
import com.personal.lifeOS.core.database.entity.EventEntity
import com.personal.lifeOS.core.database.entity.IncomeEntity
import com.personal.lifeOS.core.database.entity.MerchantCategoryEntity
import com.personal.lifeOS.core.database.entity.RecurringRuleEntity
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
    ],
    version = 6,
    exportSchema = true,
)
@TypeConverters(DateConverters::class)
abstract class LifeOSDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    abstract fun merchantCategoryDao(): MerchantCategoryDao

    abstract fun taskDao(): TaskDao

    abstract fun eventDao(): EventDao

    abstract fun budgetDao(): BudgetDao

    abstract fun incomeDao(): IncomeDao

    abstract fun recurringRuleDao(): RecurringRuleDao
}
