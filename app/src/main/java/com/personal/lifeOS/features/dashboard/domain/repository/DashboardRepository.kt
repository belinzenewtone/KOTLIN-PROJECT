package com.personal.lifeOS.features.dashboard.domain.repository

import com.personal.lifeOS.features.dashboard.domain.model.DashboardData
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun getDashboardData(): Flow<DashboardData>
}
