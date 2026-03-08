package com.personal.lifeOS.features.income.domain.repository

import com.personal.lifeOS.features.income.domain.model.IncomeRecord
import kotlinx.coroutines.flow.Flow

interface IncomeRepository {
    fun getIncomes(): Flow<List<IncomeRecord>>

    suspend fun addIncome(record: IncomeRecord): Long

    suspend fun deleteIncome(id: Long)
}
