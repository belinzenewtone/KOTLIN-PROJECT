package com.personal.lifeOS.features.feeanalytics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.core.database.dao.CategoryTotal
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.database.entity.TransactionEntity
import com.personal.lifeOS.core.security.AuthSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class FeeAnalyticsUiState(
    val categoryBreakdown: List<CategoryTotal> = emptyList(),
    val recentFeeTransactions: List<TransactionEntity> = emptyList(),
    val totalFeesThisMonth: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class FeeAnalyticsViewModel
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
        private val authSessionStore: AuthSessionStore,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(FeeAnalyticsUiState())
        val uiState: StateFlow<FeeAnalyticsUiState> = _uiState.asStateFlow()

        init {
            observeFeeData()
        }

        private fun observeFeeData() {
            viewModelScope.launch {
                val userId = authSessionStore.getUserId()
                val zone = ZoneId.systemDefault()
                val month = YearMonth.now()
                val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = month.atEndOfMonth().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

                combine(
                    transactionDao.getFeeCategoryBreakdown(start, end, userId),
                    transactionDao.getFeeTransactions(start, end, userId),
                ) { breakdown, txs ->
                    // Return a plain data triple — no side-effects inside combine transform
                    Triple(breakdown, txs, breakdown.sumOf { it.total })
                }
                    .onEach { (breakdown, txs, total) ->
                        _uiState.update {
                            it.copy(
                                categoryBreakdown = breakdown,
                                recentFeeTransactions = txs,
                                totalFeesThisMonth = total,
                                isLoading = false,
                            )
                        }
                    }
                    .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                    .launchIn(viewModelScope)
            }
        }
    }
