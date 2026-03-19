package com.personal.lifeOS.features.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.features.dashboard.domain.model.DashboardData
import com.personal.lifeOS.features.dashboard.domain.usecase.GetDashboardDataUseCase
import com.personal.lifeOS.features.insights.domain.usecase.RefreshDeterministicInsightsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val data: DashboardData = DashboardData(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        private val getDashboardDataUseCase: GetDashboardDataUseCase,
        private val refreshDeterministicInsightsUseCase: RefreshDeterministicInsightsUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DashboardUiState())
        val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

        init {
            refreshInsights()
            loadDashboard()
        }

        private fun refreshInsights() {
            viewModelScope.launch {
                refreshDeterministicInsightsUseCase()
            }
        }

        private fun loadDashboard() {
            getDashboardDataUseCase()
                .onEach { data ->
                    _uiState.update { it.copy(data = data, isLoading = false) }
                }
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .launchIn(viewModelScope)
        }
    }
