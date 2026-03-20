package com.personal.lifeOS.features.analytics.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.analytics.domain.model.AnalyticsData
import com.personal.lifeOS.features.analytics.domain.model.AnalyticsPeriod
import com.personal.lifeOS.features.analytics.domain.model.CategorySpend
import com.personal.lifeOS.features.analytics.domain.model.DailySpending
import com.personal.lifeOS.ui.components.AccentGlassCard
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.Accent
import com.personal.lifeOS.ui.theme.Error
import com.personal.lifeOS.ui.theme.Info
import com.personal.lifeOS.ui.theme.Success
import com.personal.lifeOS.ui.theme.Warning

@Composable
internal fun AnalyticsMetricRows(data: AnalyticsData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricCard(
            modifier = Modifier.weight(1f),
            title = "Monthly Spend",
            value = DateUtils.formatCurrency(data.totalSpentThisMonth),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            iconColor = MaterialTheme.colorScheme.primary,
            isAccent = true,
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            title = "Daily Average",
            value = DateUtils.formatCurrency(data.averageDailySpending),
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            iconColor = Accent,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricCard(
            modifier = Modifier.weight(1f),
            title = "Tasks Done",
            value = "${data.totalTasksCompleted}",
            icon = Icons.Filled.CheckCircle,
            iconColor = Success,
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            title = "Pending",
            value = "${data.totalTasksPending}",
            icon = Icons.Filled.Pending,
            iconColor = Warning,
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            title = "Events",
            value = "${data.totalEvents}",
            icon = Icons.Filled.CalendarMonth,
            iconColor = Info,
        )
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    isAccent: Boolean = false,
) {
    if (isAccent) {
        AccentGlassCard(modifier = modifier) {
            Column {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(8.dp))
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        GlassCard(modifier = modifier) {
            Column {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(8.dp))
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun ProductivityCard(score: Float) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { (score / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.size(64.dp),
                    color =
                        when {
                            score >= 70 -> Success
                            score >= 40 -> Warning
                            else -> Error
                        },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 6.dp,
                )
                Text(
                    "${score.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Productivity Score", style = MaterialTheme.typography.titleMedium)
                Text(
                    text =
                        when {
                            score >= 70 -> "Great job! Keep it up!"
                            score >= 40 -> "Good progress, stay focused"
                            score > 0 -> "Room for improvement"
                            else -> "Complete tasks to build your score"
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun PeriodSelector(
    selected: AnalyticsPeriod,
    onSelect: (AnalyticsPeriod) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AnalyticsPeriod.entries.forEach { period ->
            val isSelected = period == selected
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onSelect(period) }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text =
                        when (period) {
                            AnalyticsPeriod.WEEK -> "This Week"
                            AnalyticsPeriod.MONTH -> "This Month"
                        },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun SpendingBarChart(
    title: String,
    data: List<DailySpending>,
) {
    val entries = data.mapIndexed { index, day -> entryOf(index.toFloat(), day.amount.toFloat()) }
    val producer = remember { ChartEntryModelProducer() }

    LaunchedEffect(data) {
        producer.setEntries(entries)
    }

    val labels = data.map { it.dayLabel }
    val bottomAxisFormatter =
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            labels.getOrElse(value.toInt()) { "" }
        }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            Chart(
                chart =
                    columnChart(
                        columns =
                            listOf(
                                LineComponent(
                                    color = MaterialTheme.colorScheme.primary.toArgb(),
                                    thicknessDp = if (data.size <= 7) 16f else 6f,
                                    shape = Shapes.roundedCornerShape(topLeftPercent = 40, topRightPercent = 40),
                                ),
                            ),
                    ),
                chartModelProducer = producer,
                startAxis =
                    rememberStartAxis(
                        valueFormatter = { value, _ ->
                            if (value >= 1000) "${(value / 1000).toInt()}K" else value.toInt().toString()
                        },
                    ),
                bottomAxis =
                    rememberBottomAxis(
                        valueFormatter = bottomAxisFormatter,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                isZoomEnabled = false,
            )
        }
    }
}

@Composable
internal fun SpendingTrendChart(data: List<DailySpending>) {
    var cumulative = 0.0
    val cumulativeEntries =
        data.mapIndexed { index, day ->
            cumulative += day.amount
            entryOf(index.toFloat(), cumulative.toFloat())
        }

    val producer = remember { ChartEntryModelProducer() }
    LaunchedEffect(data) {
        producer.setEntries(cumulativeEntries)
    }

    val labels = data.map { it.dayLabel }
    val bottomAxisFormatter =
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            labels.getOrElse(value.toInt()) { "" }
        }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Spending Trend", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            Chart(
                chart = lineChart(),
                chartModelProducer = producer,
                startAxis =
                    rememberStartAxis(
                        valueFormatter = { value, _ ->
                            if (value >= 1000) "${(value / 1000).toInt()}K" else value.toInt().toString()
                        },
                    ),
                bottomAxis =
                    rememberBottomAxis(
                        valueFormatter = bottomAxisFormatter,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                isZoomEnabled = false,
            )
        }
    }
}

@Composable
internal fun CategoryBreakdownCard(categories: List<CategorySpend>) {
    if (categories.isEmpty()) return

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Category Breakdown", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            categories.forEach { category ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = category.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(100.dp),
                    )
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth(fraction = (category.percentage / 100f).coerceIn(0f, 1f))
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "${category.percentage.toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
