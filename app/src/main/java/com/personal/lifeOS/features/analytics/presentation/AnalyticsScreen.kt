package com.personal.lifeOS.features.analytics.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.analytics.domain.model.AnalyticsPeriod
import com.personal.lifeOS.ui.components.AccentGlassCard
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.*

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text("Analytics", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Your life metrics at a glance",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )

        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            return@Column
        }

        // Metric cards row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Monthly Spend",
                value = DateUtils.formatCurrency(state.data.totalSpentThisMonth),
                icon = Icons.Filled.TrendingUp,
                iconColor = Primary,
                isAccent = true
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Daily Average",
                value = DateUtils.formatCurrency(state.data.averageDailySpending),
                icon = Icons.Filled.TrendingDown,
                iconColor = Accent
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Tasks Done",
                value = "${state.data.totalTasksCompleted}",
                icon = Icons.Filled.CheckCircle,
                iconColor = Success
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Pending",
                value = "${state.data.totalTasksPending}",
                icon = Icons.Filled.Pending,
                iconColor = Warning
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Events",
                value = "${state.data.totalEvents}",
                icon = Icons.Filled.CalendarMonth,
                iconColor = Info
            )
        }

        // Productivity Score
        ProductivityCard(score = state.data.productivityScore)

        // Period selector
        PeriodSelector(
            selected = state.selectedPeriod,
            onSelect = { viewModel.setPeriod(it) }
        )

        // Spending Chart
        val spendingData = when (state.selectedPeriod) {
            AnalyticsPeriod.WEEK -> state.data.weeklySpending
            AnalyticsPeriod.MONTH -> state.data.monthlySpending
        }

        if (spendingData.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        if (state.selectedPeriod == AnalyticsPeriod.WEEK) "Weekly Spending" else "Monthly Spending",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(16.dp))

                    val modelProducer = remember(spendingData) {
                        CartesianChartModelProducer.build {
                            columnSeries { series(spendingData.map { it.amount }) }
                        }
                    }

                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberColumnCartesianLayer(),
                            startAxis = rememberStartAxis(),
                            bottomAxis = rememberBottomAxis()
                        ),
                        modelProducer = modelProducer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }
        }

        // Spending trend line
        if (state.data.monthlySpending.size > 1) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Spending Trend", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))

                    // Cumulative spending
                    var cumulative = 0.0
                    val cumulativeData = state.data.monthlySpending.map {
                        cumulative += it.amount
                        cumulative
                    }

                    val trendProducer = remember(cumulativeData) {
                        CartesianChartModelProducer.build {
                            lineSeries { series(cumulativeData) }
                        }
                    }

                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberLineCartesianLayer(),
                            startAxis = rememberStartAxis(),
                            bottomAxis = rememberBottomAxis()
                        ),
                        modelProducer = trendProducer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }
        }

        // Category breakdown
        if (state.data.categoryBreakdown.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Category Breakdown", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    state.data.categoryBreakdown.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                cat.category,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                modifier = Modifier.width(100.dp)
                            )
                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GlassWhite)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = (cat.percentage / 100f).coerceIn(0f, 1f))
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Primary)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "${cat.percentage.toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    isAccent: Boolean = false
) {
    val card: @Composable (Modifier, @Composable () -> Unit) -> Unit = if (isAccent) {
        { mod, content -> AccentGlassCard(modifier = mod) { content() } }
    } else {
        { mod, content -> GlassCard(modifier = mod) { content() } }
    }

    card(modifier) {
        Column {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        }
    }
}

@Composable
private fun ProductivityCard(score: Float) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Circular score indicator
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { (score / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.size(64.dp),
                    color = when {
                        score >= 70 -> Success
                        score >= 40 -> Warning
                        else -> Error
                    },
                    trackColor = GlassWhite,
                    strokeWidth = 6.dp
                )
                Text(
                    "${score.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Productivity Score", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        score >= 70 -> "Great job! Keep it up!"
                        score >= 40 -> "Good progress, stay focused"
                        score > 0 -> "Room for improvement"
                        else -> "Complete tasks to build your score"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selected: AnalyticsPeriod,
    onSelect: (AnalyticsPeriod) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AnalyticsPeriod.entries.forEach { period ->
            val isSelected = period == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Primary else GlassWhite)
                    .clickable { onSelect(period) }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = when (period) {
                        AnalyticsPeriod.WEEK -> "This Week"
                        AnalyticsPeriod.MONTH -> "This Month"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) BackgroundDark else TextSecondary
                )
            }
        }
    }
}
