package com.personal.lifeOS.features.recurring.data.repository

import com.personal.lifeOS.core.database.LocalIdGenerator
import com.personal.lifeOS.core.database.dao.RecurringRuleDao
import com.personal.lifeOS.core.database.entity.RecurringRuleEntity
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.features.recurring.domain.model.RecurringCadence
import com.personal.lifeOS.features.recurring.domain.model.RecurringRule
import com.personal.lifeOS.features.recurring.domain.model.RecurringType
import com.personal.lifeOS.features.recurring.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRepositoryImpl
    @Inject
    constructor(
        private val recurringRuleDao: RecurringRuleDao,
        private val authSessionStore: AuthSessionStore,
    ) : RecurringRepository {
        private fun activeUserId(): String = authSessionStore.getUserId()

        override fun getRules(): Flow<List<RecurringRule>> {
            return recurringRuleDao.getAll(activeUserId()).map { items ->
                items.map { it.toDomain() }
            }
        }

        override suspend fun addRule(rule: RecurringRule): Long {
            val stableId = if (rule.id > 0L) rule.id else LocalIdGenerator.nextId()
            recurringRuleDao.insert(
                rule.toEntity().copy(
                    id = stableId,
                    title = rule.title.trim(),
                    userId = activeUserId(),
                ),
            )
            return stableId
        }

        override suspend fun updateRule(rule: RecurringRule) {
            recurringRuleDao.update(
                rule.toEntity().copy(
                    title = rule.title.trim(),
                    userId = activeUserId(),
                ),
            )
        }

        override suspend fun deleteRule(id: Long) {
            recurringRuleDao.deleteById(id, activeUserId())
        }

        override suspend fun setEnabled(
            id: Long,
            enabled: Boolean,
        ) {
            recurringRuleDao.setEnabled(id = id, enabled = enabled, userId = activeUserId())
        }
    }

private fun RecurringRuleEntity.toDomain(): RecurringRule {
    val mappedType = runCatching { RecurringType.valueOf(type) }.getOrDefault(RecurringType.EXPENSE)
    val mappedCadence = runCatching { RecurringCadence.valueOf(cadence) }.getOrDefault(RecurringCadence.MONTHLY)

    return RecurringRule(
        id = id,
        title = title,
        type = mappedType,
        cadence = mappedCadence,
        nextRunAt = nextRunAt,
        amount = amount,
        enabled = enabled,
        createdAt = createdAt,
    )
}

private fun RecurringRule.toEntity(): RecurringRuleEntity {
    return RecurringRuleEntity(
        id = id,
        title = title,
        type = type.name,
        cadence = cadence.name,
        nextRunAt = nextRunAt,
        amount = amount,
        enabled = enabled,
        createdAt = createdAt,
    )
}
