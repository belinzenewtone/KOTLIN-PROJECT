package com.personal.lifeOS.features.budget.domain.repository

import com.personal.lifeOS.features.budget.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgets(): Flow<List<Budget>>

    suspend fun addBudget(budget: Budget): Long

    suspend fun updateBudget(budget: Budget)

    suspend fun deleteBudget(id: Long)
}
