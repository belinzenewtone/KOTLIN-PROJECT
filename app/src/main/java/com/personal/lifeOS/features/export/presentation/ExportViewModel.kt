package com.personal.lifeOS.features.export.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.features.export.domain.model.ExportDateRange
import com.personal.lifeOS.features.export.domain.model.ExportDomain
import com.personal.lifeOS.features.export.domain.model.ExportFormat
import com.personal.lifeOS.features.export.domain.model.ExportHistoryItem
import com.personal.lifeOS.features.export.domain.model.ExportPreview
import com.personal.lifeOS.features.export.domain.model.ExportRequest
import com.personal.lifeOS.features.export.domain.model.ExportResult
import com.personal.lifeOS.features.export.domain.usecase.BuildExportPreviewUseCase
import com.personal.lifeOS.features.export.domain.usecase.ExecuteExportUseCase
import com.personal.lifeOS.features.export.domain.usecase.ObserveExportHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class ExportDatePreset {
    ALL_TIME,
    LAST_7_DAYS,
    LAST_30_DAYS,
    THIS_MONTH,
}

data class ExportUiState(
    val selectedFormat: ExportFormat = ExportFormat.JSON,
    val selectedDomain: ExportDomain = ExportDomain.ALL,
    val selectedDatePreset: ExportDatePreset = ExportDatePreset.ALL_TIME,
    val encryptionEnabled: Boolean = false,
    val encryptionPassphrase: String = "",
    val isPreviewLoading: Boolean = false,
    val preview: ExportPreview? = null,
    val isExporting: Boolean = false,
    val result: ExportResult? = null,
    val history: List<ExportHistoryItem> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class ExportViewModel
    @Inject
    constructor(
        private val buildExportPreviewUseCase: BuildExportPreviewUseCase,
        private val executeExportUseCase: ExecuteExportUseCase,
        observeExportHistoryUseCase: ObserveExportHistoryUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ExportUiState())
        val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

        init {
            observeExportHistoryUseCase(limit = 20)
                .onEach { history ->
                    _uiState.update { it.copy(history = history) }
                }
                .launchIn(viewModelScope)
            refreshPreview()
        }

        fun setFormat(format: ExportFormat) {
            _uiState.update { state ->
                val fallbackDomain =
                    if (format == ExportFormat.CSV && state.selectedDomain == ExportDomain.ALL) {
                        ExportDomain.TRANSACTIONS
                    } else {
                        state.selectedDomain
                    }
                state.copy(
                    selectedFormat = format,
                    selectedDomain = fallbackDomain,
                    result = null,
                    error = null,
                )
            }
            refreshPreview()
        }

        fun setDomain(domain: ExportDomain) {
            _uiState.update { state ->
                val safeDomain =
                    if (state.selectedFormat == ExportFormat.CSV && domain == ExportDomain.ALL) {
                        ExportDomain.TRANSACTIONS
                    } else {
                        domain
                    }
                state.copy(
                    selectedDomain = safeDomain,
                    result = null,
                    error = null,
                )
            }
            refreshPreview()
        }

        fun setDatePreset(preset: ExportDatePreset) {
            _uiState.update {
                it.copy(
                    selectedDatePreset = preset,
                    result = null,
                    error = null,
                )
            }
            refreshPreview()
        }

        fun setEncryptionEnabled(enabled: Boolean) {
            _uiState.update { state ->
                state.copy(
                    encryptionEnabled = enabled,
                    encryptionPassphrase = if (enabled) state.encryptionPassphrase else "",
                    result = null,
                    error = null,
                )
            }
        }

        fun setEncryptionPassphrase(passphrase: String) {
            _uiState.update {
                it.copy(
                    encryptionPassphrase = passphrase,
                    result = null,
                    error = null,
                )
            }
        }

        fun export() {
            viewModelScope.launch {
                val request = currentRequest()
                _uiState.update { it.copy(isExporting = true, error = null, result = null) }
                runCatching { executeExportUseCase(request) }
                    .onSuccess { output ->
                        _uiState.update { it.copy(isExporting = false, result = output) }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isExporting = false,
                                error = error.message ?: "Export failed",
                            )
                        }
                    }
            }
        }

        private fun refreshPreview() {
            viewModelScope.launch {
                val request = currentRequest()
                _uiState.update { it.copy(isPreviewLoading = true, error = null) }
                runCatching { buildExportPreviewUseCase(request) }
                    .onSuccess { preview ->
                        _uiState.update { it.copy(isPreviewLoading = false, preview = preview) }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isPreviewLoading = false,
                                preview = null,
                                error = error.message ?: "Unable to prepare export preview.",
                            )
                        }
                    }
            }
        }

        private fun currentRequest(): ExportRequest {
            val state = _uiState.value
            return ExportRequest(
                format = state.selectedFormat,
                domain = state.selectedDomain,
                dateRange = dateRangeFromPreset(state.selectedDatePreset),
                encryptionPassphrase =
                    if (state.encryptionEnabled) {
                        state.encryptionPassphrase.ifBlank { null }
                    } else {
                        null
                    },
            )
        }

        private fun dateRangeFromPreset(preset: ExportDatePreset): ExportDateRange? {
            if (preset == ExportDatePreset.ALL_TIME) return null

            val now = System.currentTimeMillis()
            val zoneId = ZoneId.systemDefault()
            val fromMillis =
                when (preset) {
                    ExportDatePreset.ALL_TIME -> return null
                    ExportDatePreset.LAST_7_DAYS -> now - (7L * 24 * 60 * 60 * 1000)
                    ExportDatePreset.LAST_30_DAYS -> now - (30L * 24 * 60 * 60 * 1000)
                    ExportDatePreset.THIS_MONTH ->
                        LocalDate.now()
                            .withDayOfMonth(1)
                            .atStartOfDay(zoneId)
                            .toInstant()
                            .toEpochMilli()
                }
            return ExportDateRange(from = fromMillis, to = now)
        }
    }
