package com.personal.lifeOS.features.review.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.core.datastore.FeatureFlag
import com.personal.lifeOS.core.datastore.FeatureFlagStore
import com.personal.lifeOS.features.dashboard.domain.model.DashboardData
import com.personal.lifeOS.features.dashboard.domain.usecase.GetDashboardDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class ReviewPeriodSummary(
    val totalSpend: Double,
    val totalIncome: Double,
    val tasksCompleted: Int,
    val tasksPending: Int,
    val topCategory: String?,
    val weekDeltaLabel: String,
    val postureLabel: String,
)

data class ReviewRitualUiModel(
    val title: String,
    val summary: String,
    val nextStepLabel: String,
)

data class ReviewUiState(
    val greeting: String = "",
    val weekLabel: String = "",
    val summary: ReviewPeriodSummary = ReviewPeriodSummary(0.0, 0.0, 0, 0, null, "Flat week", "Steady"),
    val topInsights: List<String> = emptyList(),
    val wins: List<String> = emptyList(),
    val risks: List<String> = emptyList(),
    val ritual: ReviewRitualUiModel? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class ReviewViewModel
    @Inject
    constructor(
        private val getDashboardDataUseCase: GetDashboardDataUseCase,
        private val featureFlagStore: FeatureFlagStore,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ReviewUiState())
        val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

        private var reviewRitualsEnabled = true

        init {
            loadFlags()
            loadReview()
        }

        private fun loadFlags() {
            viewModelScope.launch {
                reviewRitualsEnabled = featureFlagStore.isEnabled(FeatureFlag.REVIEW_RITUALS)
            }
        }

        private fun loadReview() {
            getDashboardDataUseCase()
                .onEach { data ->
                    _uiState.value = buildReviewUiState(data, reviewRitualsEnabled)
                }.catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }.launchIn(viewModelScope)
        }

        private fun buildGreeting(): String {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when {
                hour < 12 -> "Good morning — here's your weekly review"
                hour < 17 -> "Good afternoon — weekly digest"
                else -> "Good evening — your week in review"
            }
        }

        private fun buildReviewUiState(
            data: DashboardData,
            ritualsEnabled: Boolean,
        ): ReviewUiState {
            val cal = Calendar.getInstance()
            val weekEnd = cal.time
            val weekLabel = "Week ending ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(weekEnd)}"
            val topCategory =
                data.recentTransactions
                    .groupBy { tx -> tx.category }
                    .maxByOrNull { (_, txs) -> txs.sumOf { tx -> tx.amount } }
                    ?.key
            val averageDailySpend = if (data.weeklySpendingData.isEmpty()) 0.0 else data.weeklySpendingData.sumOf { it.amount } / data.weeklySpendingData.size
            val delta = data.weekSpending - averageDailySpend * 5
            val posture =
                when {
                    data.pendingTaskCount > 5 || delta > 1_500.0 -> "Needs attention"
                    data.completedTodayCount > 0 && data.pendingTaskCount <= 3 -> "Strong finish"
                    else -> "Steady"
                }

            return ReviewUiState(
                greeting = buildGreeting(),
                weekLabel = weekLabel,
                summary =
                    ReviewPeriodSummary(
                        totalSpend = data.weekSpending,
                        totalIncome = data.monthIncome,
                        tasksCompleted = data.completedTodayCount,
                        tasksPending = data.pendingTaskCount,
                        topCategory = topCategory,
                        weekDeltaLabel = buildDeltaLabel(delta),
                        postureLabel = posture,
                    ),
                topInsights = data.insights.take(3).map { insight -> insight.title },
                wins = buildWins(data),
                risks = buildRisks(data, delta),
                ritual = if (ritualsEnabled) buildReviewRitual(data, delta) else null,
                isLoading = false,
            )
        }
    }

internal fun buildDeltaLabel(delta: Double): String {
    return when {
        delta > 0 -> "Up ${com.personal.lifeOS.core.utils.DateUtils.formatCurrency(delta)} from your recent pace"
        delta < 0 -> "Down ${com.personal.lifeOS.core.utils.DateUtils.formatCurrency(kotlin.math.abs(delta))} from your recent pace"
        else -> "Flat week"
    }
}

internal fun buildWins(data: DashboardData): List<String> {
    return buildList {
        if (data.completedTodayCount > 0) {
            add("${data.completedTodayCount} task${if (data.completedTodayCount == 1) "" else "s"} closed today.")
        }
        if (data.upcomingEvents.isNotEmpty()) {
            add("Your next commitment is already visible on the calendar.")
        }
        data.insights.firstOrNull()?.let { insight ->
            add(insight.title)
        }
    }.ifEmpty {
        listOf("You kept the week visible. Capture one concrete win before you close it.")
    }
}

internal fun buildRisks(
    data: DashboardData,
    delta: Double,
): List<String> {
    return buildList {
        if (data.pendingTaskCount > 0) {
            add("${data.pendingTaskCount} task${if (data.pendingTaskCount == 1) "" else "s"} still need closure.")
        }
        if (delta > 0) {
            add("Spending accelerated relative to your recent pace.")
        }
        if (data.recentTransactions.isEmpty()) {
            add("No recent finance activity is visible, so the review may be missing context.")
        }
    }.ifEmpty {
        listOf("No immediate risks stood out. Keep the same operating rhythm next week.")
    }
}

internal fun buildReviewRitual(
    data: DashboardData,
    delta: Double,
): ReviewRitualUiModel {
    val summary =
        when {
            data.pendingTaskCount > 0 ->
                "Pick the one pending task that would make next week easier, then either finish it or reschedule it deliberately."
            delta > 0 ->
                "Tag the expense category that jumped this week so the next budget conversation starts with facts."
            else ->
                "Write down one win, one risk, and one change to protect next week."
        }
    return ReviewRitualUiModel(
        title = "One thing to do before the week closes",
        summary = summary,
        nextStepLabel = "Close the ritual",
    )
}
