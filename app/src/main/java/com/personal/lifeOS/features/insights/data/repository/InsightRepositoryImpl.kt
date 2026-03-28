package com.personal.lifeOS.features.insights.data.repository

import com.personal.lifeOS.core.database.dao.InsightCardDao
import com.personal.lifeOS.core.database.dao.PaybillRegistryDao
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.database.entity.InsightCardEntity
import com.personal.lifeOS.core.database.entity.PaybillRegistryEntity
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.features.insights.domain.model.DeterministicInsightInput
import com.personal.lifeOS.features.insights.domain.model.InsightCard
import com.personal.lifeOS.features.insights.domain.model.InsightTaskSnapshot
import com.personal.lifeOS.features.insights.domain.model.InsightTransactionSnapshot
import com.personal.lifeOS.features.insights.domain.repository.InsightRepository
import com.personal.lifeOS.features.insights.domain.service.DeterministicInsightEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightRepositoryImpl
    @Inject
    constructor(
        private val insightCardDao: InsightCardDao,
        private val taskDao: TaskDao,
        private val transactionDao: TransactionDao,
        private val paybillRegistryDao: PaybillRegistryDao,
        private val authSessionStore: AuthSessionStore,
        private val deterministicInsightEngine: DeterministicInsightEngine,
    ) : InsightRepository {
        override fun observeCards(): Flow<List<InsightCard>> {
            val userId = authSessionStore.getUserId()
            if (userId.isBlank()) {
                return flowOf(emptyList())
            }

            return insightCardDao.observeAll(userId).map { cards ->
                cards.map { entity ->
                    InsightCard(
                        id = entity.id,
                        kind = entity.kind,
                        title = entity.title,
                        body = entity.body,
                        confidence = entity.confidence,
                        isAiGenerated = entity.isAiGenerated,
                        freshUntil = entity.freshUntil,
                        createdAt = entity.createdAt,
                    )
                }
            }
        }

        @Suppress("LongMethod")
        override suspend fun refreshDeterministicCards(now: Long) {
            val userId = authSessionStore.getUserId()
            if (userId.isBlank()) {
                return
            }

            insightCardDao.purgeExpired(userId = userId, now = now)
            val lookbackStart = now - Duration.ofDays(35).toMillis()

            val pendingTasks =
                taskDao.getPendingSnapshot(userId).map { task ->
                    InsightTaskSnapshot(
                        title = task.title,
                        deadline = task.deadline,
                    )
                }
            val recentTransactions =
                transactionDao.getTransactionsSnapshot(
                    userId = userId,
                    start = lookbackStart,
                    end = now,
                ).map { transaction ->
                    InsightTransactionSnapshot(
                        amount = transaction.amount,
                        category = transaction.category,
                        date = transaction.date,
                    )
                }

            val deterministicCards =
                deterministicInsightEngine.build(
                    input =
                        DeterministicInsightInput(
                            nowMillis = now,
                            pendingTasks = pendingTasks,
                            recentTransactions = recentTransactions,
                        ),
                )

            // Build engine-generated cards (6-hour freshness)
            val freshUntil = now + Duration.ofHours(6).toMillis()
            val engineEntities =
                deterministicCards.map { card ->
                    InsightCardEntity(
                        id = deterministicId(card.kind),
                        userId = userId,
                        kind = card.kind,
                        title = card.title,
                        body = card.body,
                        confidence = card.confidence,
                        isAiGenerated = false,
                        freshUntil = freshUntil,
                        createdAt = now,
                        updatedAt = now,
                        syncState = "LOCAL_ONLY",
                        recordSource = "SYSTEM",
                    )
                }

            // Build recurring paybill suggestion cards (24-hour freshness)
            val recurringFreshUntil = now + Duration.ofHours(24).toMillis()
            val frequentPaybills = paybillRegistryDao.getFrequent(userId = userId, minCount = 3)
            val recurringEntities =
                frequentPaybills.map { entry ->
                    InsightCardEntity(
                        id = deterministicId("RECURRING_SUGGESTION_${entry.paybillNumber}"),
                        userId = userId,
                        kind = "RECURRING_SUGGESTION",
                        title = "Pay ${entry.displayName}",
                        body = "Paid ${entry.usageCount} times · Last KES ${"%.0f".format(entry.lastAmountKes)}",
                        confidence = null,
                        isAiGenerated = false,
                        freshUntil = recurringFreshUntil,
                        createdAt = now,
                        updatedAt = now,
                        syncState = "LOCAL_ONLY",
                        recordSource = "SYSTEM",
                    )
                }

            val allEntities = engineEntities + recurringEntities
            insightCardDao.deleteDeterministic(userId)
            val entitiesToPersist =
                if (allEntities.isNotEmpty()) {
                    allEntities
                } else {
                    listOf(
                        InsightCardEntity(
                            id = deterministicId("BASELINE_SUMMARY"),
                            userId = userId,
                            kind = "BASELINE_SUMMARY",
                            title = "Your insights are ready",
                            body = "Keep adding tasks, events, and finance activity to unlock deeper trends.",
                            confidence = null,
                            isAiGenerated = false,
                            freshUntil = now + Duration.ofHours(6).toMillis(),
                            createdAt = now,
                            updatedAt = now,
                            syncState = "LOCAL_ONLY",
                            recordSource = "SYSTEM",
                        ),
                    )
                }
            insightCardDao.insertAll(entitiesToPersist)
        }

        private fun deterministicId(kind: String): Long {
            val hash = kind.hashCode().toLong() and 0x7fff_ffffL
            return if (hash == 0L) 1L else hash
        }
    }
