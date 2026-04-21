package com.personal.lifeOS.features.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.features.search.domain.model.SearchResult
import com.personal.lifeOS.features.search.domain.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val groupedSearchEnabled: Boolean = true,
    val error: String? = null,
    /** Most recent successful queries, newest last, capped at 5. Session-scoped (in-memory). */
    val recentSearches: List<String> = emptyList(),
)

@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val repository: SearchRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SearchUiState())
        val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
        private var searchJob: Job? = null

        fun setQuery(value: String) {
            val trimmed = value.trimStart()
            _uiState.update { it.copy(query = trimmed, error = null) }
            searchJob?.cancel()
            if (trimmed.isBlank()) {
                _uiState.update { it.copy(results = emptyList(), isLoading = false) }
                return
            }
            searchJob =
                viewModelScope.launch {
                    delay(150)
                    executeSearch(trimmed)
                }
        }

        fun runSearch() {
            val query = _uiState.value.query.trim()
            if (query.isBlank()) {
                _uiState.update { it.copy(results = emptyList(), error = null) }
                return
            }
            viewModelScope.launch { executeSearch(query) }
        }

        fun clearRecentSearches() {
            _uiState.update { it.copy(recentSearches = emptyList()) }
        }

        private suspend fun executeSearch(query: String) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.search(query) }
                .onSuccess { results ->
                    // Add to recent searches only when results come back (avoid polluting with no-result queries)
                    val updatedRecents =
                        if (results.isNotEmpty()) {
                            val without = _uiState.value.recentSearches
                                .filterNot { it.equals(query, ignoreCase = true) }
                            (without + query).takeLast(5)
                        } else {
                            _uiState.value.recentSearches
                        }
                    _uiState.update {
                        it.copy(isLoading = false, results = results, recentSearches = updatedRecents)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Search failed")
                    }
                }
        }
    }
