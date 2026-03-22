package com.personal.lifeOS.features.insights.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.features.insights.domain.model.InsightCard
import com.personal.lifeOS.features.insights.domain.usecase.ObserveInsightCardsUseCase
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

data class InsightsUiState(
    val cards: List<InsightCard> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class InsightsViewModel
    @Inject
    constructor(
        private val observeInsightCards: ObserveInsightCardsUseCase,
        private val refreshDeterministicInsights: RefreshDeterministicInsightsUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(InsightsUiState())
        val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

        init {
            refresh()
            observeCards()
        }

        private fun observeCards() {
            observeInsightCards()
                .onEach { cards ->
                    _uiState.update { it.copy(cards = cards, isLoading = false, isRefreshing = false) }
                }
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false, isRefreshing = false) }
                }
                .launchIn(viewModelScope)
        }

        fun refresh() {
            viewModelScope.launch {
                _uiState.update { it.copy(isRefreshing = true) }
                refreshDeterministicInsights()
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }

        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }
    }
