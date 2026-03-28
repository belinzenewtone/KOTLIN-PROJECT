package com.personal.lifeOS.features.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.core.datastore.FeatureFlag
import com.personal.lifeOS.core.datastore.FeatureFlagStore
import com.personal.lifeOS.core.telemetry.HealthDiagnosticsRepository
import com.personal.lifeOS.core.telemetry.SyncHealthSummary
import com.personal.lifeOS.core.update.AppUpdateInfo
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
    val syncHealth: SyncHealthSummary = SyncHealthSummary(),
    val latestUpdate: AppUpdateInfo? = null,
    val featureFlags: Map<FeatureFlag, Boolean> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        private val getDashboardDataUseCase: GetDashboardDataUseCase,
        private val refreshDeterministicInsightsUseCase: RefreshDeterministicInsightsUseCase,
        private val healthDiagnosticsRepository: HealthDiagnosticsRepository,
        private val featureFlagStore: FeatureFlagStore,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DashboardUiState())
        val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

        init {
            refreshInsights()
            loadDashboard()
            observeSyncHealth()
            observeUpdateDiagnostics()
            loadFeatureFlags()
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

        private fun observeSyncHealth() {
            healthDiagnosticsRepository.observeSyncHealth()
                .onEach { health ->
                    _uiState.update { it.copy(syncHealth = health) }
                }.launchIn(viewModelScope)
        }

        private fun observeUpdateDiagnostics() {
            healthDiagnosticsRepository.observeLatestUpdateInfo()
                .onEach { latestUpdate ->
                    _uiState.update { it.copy(latestUpdate = latestUpdate) }
                }.launchIn(viewModelScope)
        }

        private fun loadFeatureFlags() {
            viewModelScope.launch {
                runCatching { featureFlagStore.snapshot() }
                    .onSuccess { flags -> _uiState.update { it.copy(featureFlags = flags) } }
            }
        }
    }
