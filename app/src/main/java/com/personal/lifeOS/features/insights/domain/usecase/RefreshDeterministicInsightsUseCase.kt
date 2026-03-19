package com.personal.lifeOS.features.insights.domain.usecase

import com.personal.lifeOS.features.insights.domain.repository.InsightRepository
import javax.inject.Inject

class RefreshDeterministicInsightsUseCase
    @Inject
    constructor(
        private val repository: InsightRepository,
    ) {
        suspend operator fun invoke() {
            repository.refreshDeterministicCards()
        }
    }
