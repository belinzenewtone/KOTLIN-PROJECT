package com.personal.lifeOS.features.export.data.repository

import android.content.Context
import android.os.Environment
import com.google.gson.GsonBuilder
import com.personal.lifeOS.core.database.dao.BudgetDao
import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.dao.IncomeDao
import com.personal.lifeOS.core.database.dao.MerchantCategoryDao
import com.personal.lifeOS.core.database.dao.RecurringRuleDao
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.features.export.domain.model.ExportResult
import com.personal.lifeOS.features.export.domain.repository.ExportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val authSessionStore: AuthSessionStore,
        private val transactionDao: TransactionDao,
        private val taskDao: TaskDao,
        private val eventDao: EventDao,
        private val merchantCategoryDao: MerchantCategoryDao,
        private val budgetDao: BudgetDao,
        private val incomeDao: IncomeDao,
        private val recurringRuleDao: RecurringRuleDao,
    ) : ExportRepository {
        private val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .create()

        override suspend fun exportAllDataAsJson(): ExportResult {
            val userId = authSessionStore.getUserId()
            require(userId.isNotBlank()) { "Sign in required before export" }

            val payload =
                mapOf(
                    "exported_at" to System.currentTimeMillis(),
                    "user_id" to userId,
                    "transactions" to transactionDao.getAllForSync(userId).filter { it.userId == userId },
                    "tasks" to taskDao.getAllForSync(userId).filter { it.userId == userId },
                    "events" to eventDao.getAllForSync(userId).filter { it.userId == userId },
                    "merchant_categories" to merchantCategoryDao.getAllForSync(userId).filter { it.userId == userId },
                    "budgets" to budgetDao.getAllForSync(userId).filter { it.userId == userId },
                    "incomes" to incomeDao.getAllForSync(userId).filter { it.userId == userId },
                    "recurring_rules" to recurringRuleDao.getAllForSync(userId).filter { it.userId == userId },
                )

            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!directory.exists()) directory.mkdirs()

            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val file = File(directory, "beltech_export_$timestamp.json")
            file.writeText(gson.toJson(payload))

            val itemCount =
                listOf(
                    payload["transactions"],
                    payload["tasks"],
                    payload["events"],
                    payload["merchant_categories"],
                    payload["budgets"],
                    payload["incomes"],
                    payload["recurring_rules"],
                ).sumOf { (it as? List<*>)?.size ?: 0 }

            return ExportResult(
                filePath = file.absolutePath,
                itemCount = itemCount,
            )
        }
    }
