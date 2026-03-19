package com.personal.lifeOS.features.export.presentation

import com.personal.lifeOS.features.export.domain.model.ExportDomain
import com.personal.lifeOS.features.export.domain.model.ExportFormat
import com.personal.lifeOS.features.export.domain.model.ExportHistoryItem
import com.personal.lifeOS.features.export.domain.model.ExportPreview
import com.personal.lifeOS.features.export.domain.model.ExportRequest
import com.personal.lifeOS.features.export.domain.model.ExportResult
import com.personal.lifeOS.features.export.domain.repository.ExportRepository
import com.personal.lifeOS.features.export.domain.usecase.BuildExportPreviewUseCase
import com.personal.lifeOS.features.export.domain.usecase.ExecuteExportUseCase
import com.personal.lifeOS.features.export.domain.usecase.ObserveExportHistoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModelTest {
    @Test
    fun `set format to CSV remaps ALL domain to TRANSACTIONS`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = FakeExportRepository()
                val viewModel = createViewModel(repository)

                viewModel.setDomain(ExportDomain.ALL)
                viewModel.setFormat(ExportFormat.CSV)
                advanceUntilIdle()

                assertEquals(ExportFormat.CSV, viewModel.uiState.value.selectedFormat)
                assertEquals(ExportDomain.TRANSACTIONS, viewModel.uiState.value.selectedDomain)
                assertEquals(ExportDomain.TRANSACTIONS, repository.lastPreviewRequest?.domain)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `export success publishes result`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val expected =
                    ExportResult(
                        filePath = "/tmp/export.json",
                        itemCount = 3,
                        mimeType = "application/json",
                        format = ExportFormat.JSON,
                        domain = ExportDomain.ALL,
                        encrypted = false,
                        exportedAt = 1_700_000_000_000L,
                    )
                val repository = FakeExportRepository(exportResult = Result.success(expected))
                val viewModel = createViewModel(repository)

                viewModel.export()
                advanceUntilIdle()

                assertEquals(expected, viewModel.uiState.value.result)
                assertNull(viewModel.uiState.value.error)
                assertTrue(!viewModel.uiState.value.isExporting)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `export failure exposes explicit error`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = FakeExportRepository(exportResult = Result.failure(IllegalStateException("disk full")))
                val viewModel = createViewModel(repository)

                viewModel.export()
                advanceUntilIdle()

                assertNull(viewModel.uiState.value.result)
                assertEquals("disk full", viewModel.uiState.value.error)
                assertTrue(!viewModel.uiState.value.isExporting)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `history observer updates ui state`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val historyItem =
                    ExportHistoryItem(
                        id = 77L,
                        format = ExportFormat.JSON,
                        domain = ExportDomain.ALL,
                        dateRange = null,
                        filePath = "/tmp/77.json",
                        itemCount = 12,
                        encrypted = false,
                        status = "SUCCESS",
                        errorMessage = null,
                        exportedAt = 1_700_000_100_000L,
                    )
                val repository = FakeExportRepository(history = listOf(historyItem))
                val viewModel = createViewModel(repository)
                advanceUntilIdle()

                val history = viewModel.uiState.value.history
                assertEquals(1, history.size)
                assertEquals(77L, history.first().id)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `preview updates after date preset changes`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = FakeExportRepository()
                val viewModel = createViewModel(repository)

                viewModel.setDatePreset(ExportDatePreset.LAST_7_DAYS)
                advanceUntilIdle()

                assertEquals(ExportDatePreset.LAST_7_DAYS, viewModel.uiState.value.selectedDatePreset)
                assertNotNull(viewModel.uiState.value.preview)
                assertNotNull(repository.lastPreviewRequest?.dateRange)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private fun createViewModel(repository: FakeExportRepository): ExportViewModel {
        return ExportViewModel(
            buildExportPreviewUseCase = BuildExportPreviewUseCase(repository),
            executeExportUseCase = ExecuteExportUseCase(repository),
            observeExportHistoryUseCase = ObserveExportHistoryUseCase(repository),
        )
    }
}

private class FakeExportRepository(
    var previewResult: Result<ExportPreview> = Result.success(
        ExportPreview(
            request =
                ExportRequest(
                    format = ExportFormat.JSON,
                    domain = ExportDomain.ALL,
                ),
            perDomainCount = linkedMapOf(ExportDomain.TRANSACTIONS to 2),
            totalItems = 2,
        ),
    ),
    var exportResult: Result<ExportResult> = Result.success(
        ExportResult(
            filePath = "/tmp/default.json",
            itemCount = 1,
            mimeType = "application/json",
            format = ExportFormat.JSON,
            domain = ExportDomain.ALL,
            encrypted = false,
            exportedAt = 1_700_000_000_000L,
        ),
    ),
    private val history: List<ExportHistoryItem> = emptyList(),
) : ExportRepository {
    var lastPreviewRequest: ExportRequest? = null
    var lastExportRequest: ExportRequest? = null

    override suspend fun buildPreview(request: ExportRequest): ExportPreview {
        lastPreviewRequest = request
        return previewResult.getOrThrow()
    }

    override suspend fun export(request: ExportRequest): ExportResult {
        lastExportRequest = request
        return exportResult.getOrThrow()
    }

    override fun observeHistory(limit: Int): Flow<List<ExportHistoryItem>> {
        return flowOf(history.take(limit))
    }
}
