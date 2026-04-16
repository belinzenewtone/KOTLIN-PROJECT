package com.personal.lifeOS.features.recurring.data

import androidx.room.withTransaction
import com.personal.lifeOS.core.database.LifeOSDatabase
import com.personal.lifeOS.core.database.LocalIdGenerator
import com.personal.lifeOS.core.database.dao.IncomeDao
import com.personal.lifeOS.core.database.dao.RecurringRuleDao
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.database.entity.IncomeEntity
import com.personal.lifeOS.core.database.entity.TaskEntity
import com.personal.lifeOS.core.database.entity.TransactionEntity
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.features.recurring.domain.RecurringCadenceCalculator
import com.personal.lifeOS.features.recurring.domain.model.RecurringCadence
import com.personal.lifeOS.features.recurring.domain.model.RecurringType
import javax.inject.Inject
import javax.inject.Singleton

data class RecurringExecutionResult(
    val processedRules: Int,
    val createdTransactions: Int,
    val createdIncomes: Int,
    val createdTasks: Int,
    val skippedRules: Int,
)

@Singleton
class RecurringExecutionService
    @Inject
    constructor(
        private val database: LifeOSDatabase,
        private val recurringRuleDao: RecurringRuleDao,
        private val transactionDao: TransactionDao,
        private val incomeDao: IncomeDao,
        private val taskDao: TaskDao,
        private val authSessionStore: AuthSessionStore,
    ) {
        suspend fun processDueRules(maxRules: Int = 50): RecurringExecutionResult {
            val userId = authSessionStore.getUserId()
            if (userId.isBlank()) {
                return RecurringExecutionResult(
                    processedRules = 0,
                    createdTransactions = 0,
                    createdIncomes = 0,
                    createdTasks = 0,
                    skippedRules = 0,
                )
            }

            val now = System.currentTimeMillis()
            val dueRules = recurringRuleDao.getDueRules(userId = userId, now = now, limit = maxRules)

            var createdTransactions = 0
            var createdIncomes = 0
            var createdTasks = 0
            var skippedRules = 0

            dueRules.forEach { rule ->
                database.withTransaction {
                    val cadence = runCatching { RecurringCadence.valueOf(rule.cadence) }.getOrNull()
                    if (cadence == null) {
                        // Unrecognised cadence string — skip rather than silently advancing
                        // on the wrong schedule. Rule will remain due and retry next cycle.
                        skippedRules++
                        return@withTransaction
                    }
                    val type = runCatching { RecurringType.valueOf(rule.type) }.getOrNull()
                    if (type == null) {
                        skippedRules++
                        return@withTransaction
                    }
                    val nextRunAt =
                        RecurringCadenceCalculator.nextRun(
                            currentRunAt = rule.nextRunAt,
                            cadence = cadence,
                        )

                    // Atomic guard against duplicate materialization by concurrent workers.
                    val claimed =
                        recurringRuleDao.advanceIfDue(
                            id = rule.id,
                            userId = userId,
                            expectedCurrentRunAt = rule.nextRunAt,
                            nextRunAt = nextRunAt,
                        )
                    if (claimed == 0) {
                        skippedRules++
                        return@withTransaction
                    }

                    when (type) {
                        RecurringType.EXPENSE -> {
                            val amount = rule.amount
                            if (amount == null || amount <= 0.0) {
                                skippedRules++
                            } else {
                                transactionDao.insert(
                                    TransactionEntity(
                                        id = LocalIdGenerator.nextId(),
                                        amount = amount,
                                        merchant = rule.title,
                                        category = rule.category.ifBlank { "RECURRING" },
                                        date = now,
                                        source = "RECURRING",
                                        transactionType = "SENT",
                                        userId = userId,
                                    ),
                                )
                                createdTransactions++
                            }
                        }

                        RecurringType.INCOME -> {
                            val amount = rule.amount
                            if (amount == null || amount <= 0.0) {
                                skippedRules++
                            } else {
                                incomeDao.insert(
                                    IncomeEntity(
                                        id = LocalIdGenerator.nextId(),
                                        amount = amount,
                                        source = rule.title,
                                        date = now,
                                        note = "Generated from recurring rule",
                                        isRecurring = true,
                                        userId = userId,
                                    ),
                                )
                                createdIncomes++
                            }
                        }

                        RecurringType.TASK -> {
                            taskDao.insert(
                                TaskEntity(
                                    id = LocalIdGenerator.nextId(),
                                    title = rule.title,
                                    description = "Auto-generated recurring task",
                                    priority = "NEUTRAL",
                                    deadline = now + 24 * 60 * 60 * 1000L, // due end-of-next-day
                                    status = "PENDING",
                                    userId = userId,
                                ),
                            )
                            createdTasks++
                        }
                    }
                }
            }

            return RecurringExecutionResult(
                processedRules = dueRules.size,
                createdTransactions = createdTransactions,
                createdIncomes = createdIncomes,
                createdTasks = createdTasks,
                skippedRules = skippedRules,
            )
        }
    }
