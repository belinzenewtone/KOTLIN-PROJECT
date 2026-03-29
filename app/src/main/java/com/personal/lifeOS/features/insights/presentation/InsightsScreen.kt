package com.personal.lifeOS.features.insights.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.column.ColumnChart
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.EmptyState
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone
import com.personal.lifeOS.core.ui.designsystem.LoadingState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.features.insights.domain.model.InsightCard
import com.personal.lifeOS.ui.theme.AppSpacing

@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsState()

    PageScaffold(
        title = "Insights",
        subtitle = "Spending trends and habits",
        onBack = onBack,
        contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFloatingNav),
        actions = {
            IconButton(
                onClick = viewModel::refresh,
                enabled = !state.isRefreshing,
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh insights",
                )
            }
        },
    ) {
        state.error?.let {
            InlineBanner(message = it, tone = InlineBannerTone.ERROR)
        }

        if (state.isRefreshing && state.cards.isEmpty()) {
            LoadingState(label = "Analysing your data…")
            return@PageScaffold
        }

        // Stacked column chart — shown whenever we have at least one week of data
        if (state.weeklyChartData.isNotEmpty() && state.weeklyTopCategories.isNotEmpty()) {
            WeeklySpendStackedChart(
                weekData = state.weeklyChartData,
                categories = state.weeklyTopCategories,
            )
        }

        if (!state.isRefreshing && state.cards.isEmpty()) {
            EmptyState(
                title = "Insights are warming up",
                description = "Add tasks, events, or transactions to unlock trend insights.",
            )
            return@PageScaffold
        }

        state.cards.forEach { card ->
            InsightCardItem(card = card)
        }
    }
}

@Composable
private fun WeeklySpendStackedChart(
    weekData: List<WeeklySpendData>,
    categories: List<String>,
) {
    // Three visually distinct colours sourced from the active Material 3 palette
    val seriesColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
    )

    val producer = remember { ChartEntryModelProducer() }

    LaunchedEffect(weekData) {
        val seriesList = categories.mapIndexed { _, cat ->
            weekData.mapIndexed { weekIdx, week ->
                entryOf(weekIdx.toFloat(), (week.categoryAmounts[cat] ?: 0.0).toFloat())
            }
        }
        producer.setEntries(*seriesList.toTypedArray())
    }

    val weekLabels = weekData.map { it.label }
    val bottomAxisFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        weekLabels.getOrElse(value.toInt()) { "" }
    }

    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Weekly Spend by Category",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Colour legend
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                categories.forEachIndexed { i, cat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(seriesColors.getOrElse(i) { seriesColors.last() }),
                        )
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Chart(
                chart = columnChart(
                    columns = categories.indices.map { i ->
                        val color = seriesColors.getOrElse(i) { seriesColors.last() }
                        LineComponent(
                            color = color.toArgb(),
                            thicknessDp = 14f,
                            shape = Shapes.roundedCornerShape(
                                topLeftPercent = 30,
                                topRightPercent = 30,
                            ),
                        )
                    },
                    mergeMode = ColumnChart.MergeMode.Stack,
                ),
                chartModelProducer = producer,
                startAxis = rememberStartAxis(
                    valueFormatter = { value, _ ->
                        if (value >= 1000) "${(value / 1000).toInt()}K" else value.toInt().toString()
                    },
                ),
                bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisFormatter),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                isZoomEnabled = false,
            )
        }
    }
}

@Composable
private fun InsightCardItem(card: InsightCard) {
    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (card.isAiGenerated) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "AI generated",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
            Text(
                text = card.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            card.confidence?.let { conf ->
                Text(
                    text = "Confidence: ${(conf * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
