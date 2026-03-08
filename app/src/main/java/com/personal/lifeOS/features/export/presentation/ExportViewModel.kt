package com.personal.lifeOS.features.export.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.features.export.domain.model.ExportResult
import com.personal.lifeOS.features.export.domain.repository.ExportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExportUiState(
    val isExporting: Boolean = false,
    val result: ExportResult? = null,
    val error: String? = null,
)

@HiltViewModel
class ExportViewModel
    @Inject
    constructor(
        private val repository: ExportRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ExportUiState())
        val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

        fun exportJson() {
            viewModelScope.launch {
                _uiState.update { it.copy(isExporting = true, error = null, result = null) }
                runCatching { repository.exportAllDataAsJson() }
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
    }
