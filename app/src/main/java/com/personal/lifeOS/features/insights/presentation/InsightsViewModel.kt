package com.personal.lifeOS.features.insights.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.security.AuthSessionStore
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Per-week spending split by top-3 categories — drives the stacked column chart.
 * [categoryAmounts] keys are ordered from highest to lowest total spend across all weeks.
 */
data class WeeklySpendData(
    val label: String,
    val categoryAmounts: Map<String, Double>,
)

/** Monthly spending total — drives the 6-month trend list. */
data class MonthlySpendData(
    val label: String,           // "Jan 2025"
    val totalSpend: Double,
    val previousTotal: Double,   // prior month, for delta indicator
)

data class InsightsUiState(
    val cards: List<InsightCard> = emptyList(),
    /** Weekly spend data for the stacked column chart (last 5 weeks). Empty while loading. */
    val weeklyChartData: List<WeeklySpendData> = emptyList(),
    /** Top-3 category names corresponding to the series in [weeklyChartData]. */
    val weeklyTopCategories: List<String> = emptyList(),
    /** Monthly totals for the last 6 months, oldest → newest. */
    val monthlySpendData: List<MonthlySpendData> = emptyList(),
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
        private val transactionDao: TransactionDao,
        private val authSessionStore: AuthSessionStore,
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
                launch { loadWeeklyChartData() }
                launch { loadMonthlySpendData() }
                runCatching { refreshDeterministicInsights() }
                    .onSuccess {
                        _uiState.update { it.copy(isRefreshing = false, error = null) }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isRefreshing = false,
                                error = error.message ?: "Unable to refresh insights right now.",
                            )
                        }
                    }
            }
        }

        /**
         * Loads the last 5 weeks of spending, splits each week by the top-3 categories
         * (ranked by total across all weeks), and stores the result in [InsightsUiState].
         *
         * Income transactions (RECEIVED / DEPOSIT) are excluded so only outflows are charted.
         */
        private suspend fun loadWeeklyChartData() {
            try {
                val userId = authSessionStore.getUserId()
                val now = System.currentTimeMillis()
                val weekMs = 7L * 24L * 60L * 60L * 1000L
                val incomeTypes = setOf("RECEIVED", "DEPOSIT")

                // Collect raw transactions for each of the last 5 weeks
                val rawWeeks = (4 downTo 0).map { offset ->
                    val end = now - offset * weekMs
                    val start = end - weekMs
                    val label = when (offset) {
                        0 -> "This wk"
                        1 -> "Last wk"
                        else -> "W-${offset}"
                    }
                    val txs = transactionDao.getTransactionsSnapshot(userId, start, end)
                        .filter { it.amount > 0.0 && it.transactionType !in incomeTypes }
                    label to txs
                }

                // Identify global top-3 expense categories
                val top3 = rawWeeks.flatMap { it.second }
                    .groupBy { it.category }
                    .entries
                    .sortedByDescending { e -> e.value.sumOf { it.amount } }
                    .take(3)
                    .map { it.key }

                val chartData = rawWeeks.map { (label, txs) ->
                    WeeklySpendData(
                        label = label,
                        categoryAmounts = top3.associateWith { cat ->
                            txs.filter { it.category == cat }.sumOf { it.amount }
                        },
                    )
                }

                _uiState.update { it.copy(weeklyChartData = chartData, weeklyTopCategories = top3) }
            } catch (_: Exception) {
                // Chart failing silently is fine — insights cards still show
            }
        }

        /**
         * Loads the last 6 full calendar months of spending totals (outflows only).
         *
         * Optimisations vs a naïve approach:
         *  - Uses [TransactionDao.getTotalSpendingBetween] which issues a single SQL SUM() per
         *    month — no full entity list is loaded into memory.
         *  - All 6 month queries are launched in parallel via [kotlinx.coroutines.async] so total
         *    latency ≈ one query rather than six chained queries.
         *  - The [DateTimeFormatter] pattern and [java.time.ZoneId] are resolved once, outside
         *    the parallel jobs, to avoid redundant work.
         *
         * The result drives the Monthly Trend section in InsightsScreen.
         */
        private suspend fun loadMonthlySpendData() {
            try {
                val userId = authSessionStore.getUserId()
                val zone = java.time.ZoneId.systemDefault()
                val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")

                // Describe each month's window *before* launching parallel jobs
                val windows = (5 downTo 0).map { offset ->
                    val ym = java.time.YearMonth.now().minusMonths(offset.toLong())
                    val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = ym.atEndOfMonth().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                    Triple(ym.format(formatter), start, end)
                }

                // Run all 6 SQL SUM() queries in parallel — latency ≈ 1 query, not 6
                val totals: List<Pair<String, Double>> = coroutineScope {
                    windows.map { (label, start, end) ->
                        async {
                            val spend = transactionDao.getTotalSpendingBetweenSnapshot(
                                start = start,
                                end = end,
                                userId = userId,
                            ) ?: 0.0
                            label to spend
                        }
                    }.map { it.await() }
                }

                val monthlyData = totals.mapIndexed { idx, (label, total) ->
                    MonthlySpendData(
                        label = label,
                        totalSpend = total,
                        previousTotal = if (idx > 0) totals[idx - 1].second else 0.0,
                    )
                }

                _uiState.update { it.copy(monthlySpendData = monthlyData) }
            } catch (_: Exception) {
                // Non-fatal — monthly trend is optional; insight cards still show
            }
        }

        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }
    }
