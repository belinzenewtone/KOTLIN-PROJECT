package com.personal.lifeOS.features.income.data.repository

import com.personal.lifeOS.core.database.LocalIdGenerator
import com.personal.lifeOS.core.database.dao.IncomeDao
import com.personal.lifeOS.core.database.entity.IncomeEntity
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.features.income.domain.model.IncomeRecord
import com.personal.lifeOS.features.income.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomeRepositoryImpl
    @Inject
    constructor(
        private val incomeDao: IncomeDao,
        private val authSessionStore: AuthSessionStore,
    ) : IncomeRepository {
        private fun activeUserId(): String = authSessionStore.getUserId()

        override fun getIncomes(): Flow<List<IncomeRecord>> {
            return incomeDao.getAll(activeUserId()).map { items ->
                items.map { it.toDomain() }
            }
        }

        override suspend fun addIncome(record: IncomeRecord): Long {
            val stableId = if (record.id > 0L) record.id else LocalIdGenerator.nextId()
            incomeDao.insert(
                record.toEntity().copy(
                    id = stableId,
                    source = record.source.trim(),
                    userId = activeUserId(),
                ),
            )
            return stableId
        }

        override suspend fun deleteIncome(id: Long) {
            incomeDao.deleteById(id, activeUserId())
        }
    }

private fun IncomeEntity.toDomain(): IncomeRecord {
    return IncomeRecord(
        id = id,
        amount = amount,
        source = source,
        date = date,
        note = note,
        isRecurring = isRecurring,
    )
}

private fun IncomeRecord.toEntity(): IncomeEntity {
    return IncomeEntity(
        id = id,
        amount = amount,
        source = source,
        date = date,
        note = note,
        isRecurring = isRecurring,
    )
}
