package com.personal.lifeOS.core.database

import com.personal.lifeOS.core.database.dao.BudgetDao
import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.dao.IncomeDao
import com.personal.lifeOS.core.database.dao.MerchantCategoryDao
import com.personal.lifeOS.core.database.dao.RecurringRuleDao
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Claims legacy rows that were created before user ownership was added.
 */
@Singleton
class UserDataOwnershipService
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
        private val taskDao: TaskDao,
        private val eventDao: EventDao,
        private val merchantCategoryDao: MerchantCategoryDao,
        private val budgetDao: BudgetDao,
        private val incomeDao: IncomeDao,
        private val recurringRuleDao: RecurringRuleDao,
    ) {
        suspend fun claimUnownedData(userId: String) {
            if (userId.isBlank()) return
            runCatching { transactionDao.claimUnownedRecords(userId) }
            runCatching { taskDao.claimUnownedRecords(userId) }
            runCatching { eventDao.claimUnownedRecords(userId) }
            runCatching { merchantCategoryDao.claimUnownedRecords(userId) }
            runCatching { budgetDao.claimUnownedRecords(userId) }
            runCatching { incomeDao.claimUnownedRecords(userId) }
            runCatching { recurringRuleDao.claimUnownedRecords(userId) }
        }
    }
