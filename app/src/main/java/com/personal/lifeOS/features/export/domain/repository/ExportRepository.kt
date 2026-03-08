package com.personal.lifeOS.features.export.domain.repository

import com.personal.lifeOS.features.export.domain.model.ExportResult

interface ExportRepository {
    suspend fun exportAllDataAsJson(): ExportResult
}
