package com.personal.lifeOS.features.insights.domain.repository

import com.personal.lifeOS.features.insights.domain.model.InsightCard
import kotlinx.coroutines.flow.Flow

interface InsightRepository {
    fun observeCards(): Flow<List<InsightCard>>

    suspend fun refreshDeterministicCards(now: Long = System.currentTimeMillis())
}
