package com.personal.lifeOS.features.export.data.repository

import com.personal.lifeOS.core.database.LifeOSDatabase
import com.personal.lifeOS.core.database.LocalIdGenerator
import com.personal.lifeOS.core.database.entity.BudgetEntity
import com.personal.lifeOS.core.database.entity.EventEntity
import com.personal.lifeOS.core.database.entity.ExportHistoryEntity
import com.personal.lifeOS.core.database.entity.IncomeEntity
import com.personal.lifeOS.core.database.entity.MerchantCategoryEntity
import com.personal.lifeOS.core.database.entity.RecurringRuleEntity
import com.personal.lifeOS.core.database.entity.TaskEntity
import com.personal.lifeOS.core.database.entity.TransactionEntity
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.features.export.domain.model.ExportDateRange
import com.personal.lifeOS.features.export.domain.model.ExportDomain
import com.personal.lifeOS.features.export.domain.model.ExportFormat
import com.personal.lifeOS.features.export.domain.model.ExportHistoryItem
import com.personal.lifeOS.features.export.domain.model.ExportPreview
import com.personal.lifeOS.features.export.domain.model.ExportRequest
import com.personal.lifeOS.features.export.domain.model.ExportResult
import com.personal.lifeOS.features.export.domain.repository.ExportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportRepositoryImpl
    @Inject
    constructor(
        private val authSessionStore: AuthSessionStore,
        private val database: LifeOSDatabase,
        private val payloadSerializer: ExportPayloadSerializer,
        private val fileWriter: ExportFileWriter,
    ) : ExportRepository {
        override suspend fun buildPreview(request: ExportRequest): ExportPreview {
            validateRequest(request)
            val userId = activeUserId()
            val bundle = loadBundle(userId, request)
            return ExportPreview(
                request = request,
                perDomainCount = bundle.domainCounts(),
                totalItems = bundle.totalItemCount(),
            )
        }

        override suspend fun export(request: ExportRequest): ExportResult {
            validateRequest(request)
            val userId = activeUserId()
            val exportedAt = System.currentTimeMillis()

            return runCatching {
                val bundle = loadBundle(userId, request)
                val plainBytes = payloadSerializer.serialize(request, bundle, userId, exportedAt)
                val (fileBytes, encrypted) =
                    fileWriter.encodeForStorage(
                        bytes = plainBytes,
                        passphrase = request.encryptionPassphrase,
                    )
                val file =
                    fileWriter.writeFile(
                        request = request,
                        bytes = fileBytes,
                    )

                val result =
                    ExportResult(
                        filePath = file.absolutePath,
                        itemCount = bundle.totalItemCount(),
                        mimeType = payloadSerializer.mimeType(request.format, encrypted),
                        format = request.format,
                        domain = request.domain,
                        encrypted = encrypted,
                        exportedAt = exportedAt,
                    )

                persistHistory(
                    userId = userId,
                    request = request,
                    exportedAt = exportedAt,
                    itemCount = result.itemCount,
                    filePath = result.filePath,
                    status = "SUCCESS",
                    errorMessage = null,
                )
                result
            }.getOrElse { error ->
                persistHistory(
                    userId = userId,
                    request = request,
                    exportedAt = exportedAt,
                    itemCount = 0,
                    filePath = null,
                    status = "FAILED",
                    errorMessage = error.message,
                )
                throw error
            }
        }

        override fun observeHistory(limit: Int): Flow<List<ExportHistoryItem>> {
            val userId = authSessionStore.getUserId()
            if (userId.isBlank()) return flowOf(emptyList())

            return database.exportHistoryDao().observeRecent(userId = userId, limit = limit).map { entities ->
                entities.map { entity -> entity.toDomain() }
            }
        }

        private suspend fun loadBundle(
            userId: String,
            request: ExportRequest,
        ): ExportBundle {
            val range = request.dateRange
            val transactionDao = database.transactionDao()
            val taskDao = database.taskDao()
            val eventDao = database.eventDao()
            val budgetDao = database.budgetDao()
            val incomeDao = database.incomeDao()
            val recurringRuleDao = database.recurringRuleDao()
            val merchantCategoryDao = database.merchantCategoryDao()

            val bundle =
                ExportBundle(
                    transactions =
                        transactionDao.getAllForSync(userId)
                            .filter { it.isOwnedBy(userId) }
                            .filterByRange(range) { it.date },
                    tasks =
                        taskDao.getAllForSync(userId)
                            .filter { it.isOwnedBy(userId) }
                            .filterByRange(range) { it.deadline ?: it.createdAt },
                    events =
                        eventDao.getAllForSync(userId)
                            .filter { it.isOwnedBy(userId) }
                            .filterByRange(range) { it.date },
                    budgets =
                        budgetDao.getAllForSync(userId)
                            .filter { it.isOwnedBy(userId) }
                            .filterByRange(range) { it.createdAt },
                    incomes =
                        incomeDao.getAllForSync(userId)
                            .filter { it.isOwnedBy(userId) }
                            .filterByRange(range) { it.date },
                    recurringRules =
                        recurringRuleDao.getAllForSync(userId)
                            .filter { it.isOwnedBy(userId) }
                            .filterByRange(range) { it.createdAt },
                    merchantRules =
                        merchantCategoryDao.getAllForSync(userId)
                            .filter { it.isOwnedBy(userId) }
                            .filterByRange(range) { it.updatedAt },
                )
            return bundle.select(request.domain)
        }

        private suspend fun persistHistory(
            userId: String,
            request: ExportRequest,
            exportedAt: Long,
            itemCount: Int,
            filePath: String?,
            status: String,
            errorMessage: String?,
        ) {
            database.exportHistoryDao().insert(
                ExportHistoryEntity(
                    id = LocalIdGenerator.nextId(),
                    userId = userId,
                    format = request.format.name,
                    domainScope = request.domain.name,
                    dateFrom = request.dateRange?.from,
                    dateTo = request.dateRange?.to,
                    filePath = filePath,
                    itemCount = itemCount,
                    isEncrypted = !request.encryptionPassphrase.isNullOrBlank(),
                    status = status,
                    errorMessage = errorMessage,
                    exportedAt = exportedAt,
                    createdAt = exportedAt,
                    updatedAt = exportedAt,
                ),
            )
        }

        private fun activeUserId(): String {
            val userId = authSessionStore.getUserId()
            require(userId.isNotBlank()) { "Sign in required before export." }
            return userId
        }

        private fun validateRequest(request: ExportRequest) {
            if (request.format == ExportFormat.CSV) {
                require(request.domain != ExportDomain.ALL) { "CSV export requires a specific domain." }
            }
            request.dateRange?.let { range ->
                require(range.from <= range.to) { "Invalid date range. Start must be before end." }
            }
        }
    }

private fun ExportHistoryEntity.toDomain(): ExportHistoryItem {
    return ExportHistoryItem(
        id = id,
        format = runCatching { ExportFormat.valueOf(format) }.getOrElse { ExportFormat.JSON },
        domain = runCatching { ExportDomain.valueOf(domainScope) }.getOrElse { ExportDomain.ALL },
        dateRange =
            if (dateFrom != null && dateTo != null) {
                ExportDateRange(from = dateFrom, to = dateTo)
            } else {
                null
            },
        filePath = filePath,
        itemCount = itemCount,
        encrypted = isEncrypted,
        status = status,
        errorMessage = errorMessage,
        exportedAt = exportedAt,
    )
}

private fun TransactionEntity.isOwnedBy(userId: String): Boolean = this.userId == userId && deletedAt == null

private fun TaskEntity.isOwnedBy(userId: String): Boolean = this.userId == userId && deletedAt == null

private fun EventEntity.isOwnedBy(userId: String): Boolean = this.userId == userId && deletedAt == null

private fun BudgetEntity.isOwnedBy(userId: String): Boolean = this.userId == userId && deletedAt == null

private fun IncomeEntity.isOwnedBy(userId: String): Boolean = this.userId == userId && deletedAt == null

private fun RecurringRuleEntity.isOwnedBy(userId: String): Boolean = this.userId == userId && deletedAt == null

private fun MerchantCategoryEntity.isOwnedBy(userId: String): Boolean = this.userId == userId && deletedAt == null

private inline fun <T> List<T>.filterByRange(
    range: ExportDateRange?,
    timestampOf: (T) -> Long,
): List<T> {
    if (range == null) return this
    return filter { item ->
        timestampOf(item) in range.from..range.to
    }
}
