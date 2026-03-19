package com.personal.lifeOS.feature.finance.domain.usecase

import com.personal.lifeOS.feature.finance.domain.model.FinanceSnapshot
import com.personal.lifeOS.feature.finance.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFinanceSnapshotUseCase
    @Inject
    constructor(
        private val financeRepository: FinanceRepository,
    ) {
        operator fun invoke(): Flow<FinanceSnapshot> {
            return financeRepository.observeSnapshot()
        }
    }
