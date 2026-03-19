package com.personal.lifeOS.features.expenses.domain.usecase

import com.personal.lifeOS.core.telemetry.HealthDiagnosticsRepository
import com.personal.lifeOS.core.telemetry.ImportHealthSummary
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveImportHealthUseCase
    @Inject
    constructor(
        private val healthDiagnosticsRepository: HealthDiagnosticsRepository,
    ) {
        operator fun invoke(): Flow<ImportHealthSummary> {
            return healthDiagnosticsRepository.observeImportHealth()
        }
    }

