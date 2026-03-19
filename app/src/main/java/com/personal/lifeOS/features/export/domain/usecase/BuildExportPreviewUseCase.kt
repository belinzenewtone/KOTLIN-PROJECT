package com.personal.lifeOS.features.export.domain.usecase

import com.personal.lifeOS.features.export.domain.model.ExportPreview
import com.personal.lifeOS.features.export.domain.model.ExportRequest
import com.personal.lifeOS.features.export.domain.repository.ExportRepository
import javax.inject.Inject

class BuildExportPreviewUseCase
    @Inject
    constructor(
        private val repository: ExportRepository,
    ) {
        suspend operator fun invoke(request: ExportRequest): ExportPreview {
            return repository.buildPreview(request)
        }
    }
