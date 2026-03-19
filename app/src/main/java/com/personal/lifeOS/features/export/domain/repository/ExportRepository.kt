package com.personal.lifeOS.features.export.domain.repository

import com.personal.lifeOS.features.export.domain.model.ExportHistoryItem
import com.personal.lifeOS.features.export.domain.model.ExportPreview
import com.personal.lifeOS.features.export.domain.model.ExportRequest
import com.personal.lifeOS.features.export.domain.model.ExportResult
import kotlinx.coroutines.flow.Flow

interface ExportRepository {
    suspend fun buildPreview(request: ExportRequest): ExportPreview

    suspend fun export(request: ExportRequest): ExportResult

    fun observeHistory(limit: Int = 20): Flow<List<ExportHistoryItem>>
}
