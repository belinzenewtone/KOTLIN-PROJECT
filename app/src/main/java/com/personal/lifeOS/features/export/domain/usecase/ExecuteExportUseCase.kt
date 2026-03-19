package com.personal.lifeOS.features.export.domain.usecase

import com.personal.lifeOS.features.export.domain.model.ExportRequest
import com.personal.lifeOS.features.export.domain.model.ExportResult
import com.personal.lifeOS.features.export.domain.repository.ExportRepository
import javax.inject.Inject

class ExecuteExportUseCase
    @Inject
    constructor(
        private val repository: ExportRepository,
    ) {
        suspend operator fun invoke(request: ExportRequest): ExportResult {
            return repository.export(request)
        }
    }
