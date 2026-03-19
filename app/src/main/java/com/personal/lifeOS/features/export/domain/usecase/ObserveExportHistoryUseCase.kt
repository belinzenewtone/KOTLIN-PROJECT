package com.personal.lifeOS.features.export.domain.usecase

import com.personal.lifeOS.features.export.domain.model.ExportHistoryItem
import com.personal.lifeOS.features.export.domain.repository.ExportRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveExportHistoryUseCase
    @Inject
    constructor(
        private val repository: ExportRepository,
    ) {
        operator fun invoke(limit: Int = 20): Flow<List<ExportHistoryItem>> {
            return repository.observeHistory(limit = limit)
        }
    }
