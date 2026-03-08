package com.personal.lifeOS.features.budget.data.repository

import com.personal.lifeOS.core.database.LocalIdGenerator
import com.personal.lifeOS.core.database.dao.BudgetDao
import com.personal.lifeOS.core.database.entity.BudgetEntity
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.features.budget.domain.model.Budget
import com.personal.lifeOS.features.budget.domain.model.BudgetPeriod
import com.personal.lifeOS.features.budget.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl
    @Inject
    constructor(
        private val budgetDao: BudgetDao,
        private val authSessionStore: AuthSessionStore,
    ) : BudgetRepository {
        private fun activeUserId(): String = authSessionStore.getUserId()

        override fun getBudgets(): Flow<List<Budget>> {
            return budgetDao.getAll(activeUserId()).map { items ->
                items.map { it.toDomain() }
            }
        }

        override suspend fun addBudget(budget: Budget): Long {
            val stableId = if (budget.id > 0L) budget.id else LocalIdGenerator.nextId()
            budgetDao.insert(
                budget.toEntity().copy(
                    id = stableId,
                    userId = activeUserId(),
                    category = budget.category.trim().uppercase(),
                ),
            )
            return stableId
        }

        override suspend fun updateBudget(budget: Budget) {
            budgetDao.update(
                budget.toEntity().copy(
                    userId = activeUserId(),
                    category = budget.category.trim().uppercase(),
                ),
            )
        }

        override suspend fun deleteBudget(id: Long) {
            budgetDao.deleteById(id, activeUserId())
        }
    }

private fun BudgetEntity.toDomain(): Budget {
    return Budget(
        id = id,
        category = category,
        limitAmount = limitAmount,
        period =
            try {
                BudgetPeriod.valueOf(period)
            } catch (_: Exception) {
                BudgetPeriod.MONTHLY
            },
        createdAt = createdAt,
    )
}

private fun Budget.toEntity(): BudgetEntity {
    return BudgetEntity(
        id = id,
        category = category,
        limitAmount = limitAmount,
        period = period.name,
        createdAt = createdAt,
    )
}
