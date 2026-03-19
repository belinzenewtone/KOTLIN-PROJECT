package com.personal.lifeOS.bootstrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppBootstrapViewModel
    @Inject
    constructor(
        private val appBootstrapCoordinator: AppBootstrapCoordinator,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AppBootstrapUiState())
        val uiState: StateFlow<AppBootstrapUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val result = appBootstrapCoordinator.bootstrap()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        result = result,
                    )
                }
            }
        }
    }
