package com.personal.lifeOS.feature.finance.domain.repository

import com.personal.lifeOS.feature.finance.domain.model.FinanceSnapshot
import kotlinx.coroutines.flow.Flow

interface FinanceRepository {
    fun observeSnapshot(): Flow<FinanceSnapshot>
}
