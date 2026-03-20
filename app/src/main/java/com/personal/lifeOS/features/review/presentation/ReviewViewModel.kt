package com.personal.lifeOS.features.review.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.features.dashboard.domain.usecase.GetDashboardDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.util.Calendar
import javax.inject.Inject

data class ReviewPeriodSummary(
    val totalSpend: Double,
    val totalIncome: Double,
    val tasksCompleted: Int,
    val tasksPending: Int,
    val topCategory: String?,
)

data class ReviewUiState(
    val greeting: String = "",
    val weekLabel: String = "",
    val summary: ReviewPeriodSummary = ReviewPeriodSummary(0.0, 0.0, 0, 0, null),
    val topInsights: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class ReviewViewModel
    @Inject
    constructor(
        private val getDashboardDataUseCase: GetDashboardDataUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ReviewUiState())
        val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

        init {
            loadReview()
        }

        private fun loadReview() {
            getDashboardDataUseCase()
                .onEach { data ->
                    val cal = Calendar.getInstance()
                    val weekEnd = cal.time
                    cal.add(Calendar.DAY_OF_YEAR, -7)
                    val weekStart = cal.time

                    _uiState.update {
                        it.copy(
                            greeting = buildGreeting(),
                            weekLabel = "Week ending ${java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(weekEnd)}",
                            summary = ReviewPeriodSummary(
                                totalSpend = data.weekSpending,
                                totalIncome = 0.0, // wired when IncomeRepository exposes weekly total
                                tasksCompleted = data.completedTodayCount,
                                tasksPending = data.pendingTaskCount,
                                topCategory = data.recentTransactions
                                    .groupBy { tx -> tx.category }
                                    .maxByOrNull { (_, txs) -> txs.sumOf { tx -> tx.amount } }
                                    ?.key,
                            ),
                            topInsights = data.insights.take(3).map { insight -> insight.title },
                            isLoading = false,
                        )
                    }
                }
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .launchIn(viewModelScope)
        }

        private fun buildGreeting(): String {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when {
                hour < 12 -> "Good morning — here's your weekly review"
                hour < 17 -> "Good afternoon — weekly digest"
                else -> "Good evening — your week in review"
            }
        }
    }
