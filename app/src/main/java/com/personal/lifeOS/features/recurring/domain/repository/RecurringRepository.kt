package com.personal.lifeOS.features.recurring.domain.repository

import com.personal.lifeOS.features.recurring.domain.model.RecurringRule
import kotlinx.coroutines.flow.Flow

interface RecurringRepository {
    fun getRules(): Flow<List<RecurringRule>>

    suspend fun addRule(rule: RecurringRule): Long

    suspend fun updateRule(rule: RecurringRule)

    suspend fun deleteRule(id: Long)

    suspend fun setEnabled(
        id: Long,
        enabled: Boolean,
    )
}
