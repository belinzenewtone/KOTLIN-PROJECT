package com.personal.lifeOS.features.analytics.domain.repository

import com.personal.lifeOS.features.analytics.domain.model.AnalyticsData
import kotlinx.coroutines.flow.Flow

interface AnalyticsRepository {
    fun getAnalytics(): Flow<AnalyticsData>
}
