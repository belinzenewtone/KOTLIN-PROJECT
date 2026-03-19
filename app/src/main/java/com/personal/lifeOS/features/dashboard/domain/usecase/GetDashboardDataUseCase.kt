package com.personal.lifeOS.features.dashboard.domain.usecase

import com.personal.lifeOS.features.dashboard.domain.model.DashboardData
import com.personal.lifeOS.features.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDashboardDataUseCase
    @Inject
    constructor(
        private val repository: DashboardRepository,
    ) {
        operator fun invoke(): Flow<DashboardData> = repository.getDashboardData()
    }
