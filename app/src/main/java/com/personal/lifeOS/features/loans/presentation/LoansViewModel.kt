package com.personal.lifeOS.features.loans.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.core.database.entity.FulizaLoanEntity
import com.personal.lifeOS.features.expenses.domain.repository.FulizaLoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class LoansUiState(
    val openLoans: List<FulizaLoanEntity> = emptyList(),
    val closedLoans: List<FulizaLoanEntity> = emptyList(),
    val netOutstanding: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class LoansViewModel
    @Inject
    constructor(
        private val fulizaLoanRepository: FulizaLoanRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LoansUiState())
        val uiState: StateFlow<LoansUiState> = _uiState.asStateFlow()

        init {
            observeLoans()
        }

        private fun observeLoans() {
            combine(
                fulizaLoanRepository.observeOpenLoans(),
                fulizaLoanRepository.observeNetOutstanding(),
            ) { loans, outstanding ->
                val open = loans.filter { it.status in setOf("OPEN", "PARTIALLY_REPAID") }
                    .sortedByDescending { it.drawDate }
                val closed = loans.filter { it.status == "CLOSED" }
                    .sortedByDescending { it.lastRepaymentDate ?: it.updatedAt }
                _uiState.update {
                    it.copy(
                        openLoans = open,
                        closedLoans = closed,
                        netOutstanding = outstanding,
                        isLoading = false,
                    )
                }
            }
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .launchIn(viewModelScope)
        }
    }
