package com.personal.lifeOS.features.insights.domain.usecase

import com.personal.lifeOS.features.insights.domain.model.InsightCard
import com.personal.lifeOS.features.insights.domain.repository.InsightRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveInsightCardsUseCase
    @Inject
    constructor(
        private val repository: InsightRepository,
    ) {
        operator fun invoke(): Flow<List<InsightCard>> = repository.observeCards()
    }
