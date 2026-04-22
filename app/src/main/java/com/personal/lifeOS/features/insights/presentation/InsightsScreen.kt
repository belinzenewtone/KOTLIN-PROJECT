@file:Suppress("LongMethod", "MagicNumber")

package com.personal.lifeOS.features.insights.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
        headerEyebrow = "Trends",
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
                    imageVector = Icons.Outlined.Refresh,
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

        if (state.weeklyChartData.isNotEmpty() && state.weeklyTopCategories.isNotEmpty()) {
            WeeklySpendBarChart(
                weekData = state.weeklyChartData,
                categories = state.weeklyTopCategories,
            )
        }

        if (state.monthlySpendData.isNotEmpty()) {
            MonthlySpendTrendSection(months = state.monthlySpendData)
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

// ─────────────────────────────────────────────────────────────────────────────
// Modern stacked bar chart — drawn entirely in Canvas for precise control over
// gradients, rounded corners and depth.  A touch tooltip appears on press/hold
// without being intrusive.
// ─────────────────────────────────────────────────────────────────────────────

private const val CHART_HEIGHT_DP = 224
private const val LABEL_ROW_DP = 26
private const val GUIDE_LINES = 4
private const val BAR_WIDTH_FRACTION = 0.48f   // bar width as a fraction of group width
private const val TOP_RADIUS_DP = 7f

@Composable
private fun WeeklySpendBarChart(
    weekData: List<WeeklySpendData>,
    categories: List<String>,
) {
    val seriesColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
    )

    // ── Touch state ───────────────────────────────────────────────────────────
    var pressedIdx by remember { mutableStateOf<Int?>(null) }
    var tooltipX by remember { mutableFloatStateOf(0f) }    // px from chart left

    // Animate the pressed bar's scale for subtle feedback
    val scales = List(weekData.size) { idx ->
        animateFloatAsState(
            targetValue = if (pressedIdx == idx) 1.035f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "barScale$idx",
        ).value
    }

    // ── Theme colours captured for Canvas ────────────────────────────────────
    val guideColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tooltipSurface = MaterialTheme.colorScheme.surfaceContainerHighest
    val tooltipText = MaterialTheme.colorScheme.onSurface

    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // Pre-compute max total so all weeks use the same scale
    val maxTotal = remember(weekData, categories) {
        weekData.maxOfOrNull { w -> categories.sumOf { c -> w.categoryAmounts[c] ?: 0.0 } }
            ?.toFloat()
            ?.coerceAtLeast(1f) ?: 1f
    }

    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // ── Title ─────────────────────────────────────────────────────────
            Text(
                text = "Weekly Spend by Category",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // ── Legend ────────────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                categories.forEachIndexed { i, cat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
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

            Spacer(Modifier.height(2.dp))

            // ── Chart + Tooltip ───────────────────────────────────────────────
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CHART_HEIGHT_DP.dp),
            ) {
                val chartWidthPx = constraints.maxWidth.toFloat()
                val chartHeightPx = with(density) { CHART_HEIGHT_DP.dp.toPx() }
                val labelRowPx = with(density) { LABEL_ROW_DP.dp.toPx() }
                val barsAreaH = chartHeightPx - labelRowPx
                val groupW = chartWidthPx / weekData.size.coerceAtLeast(1)
                val barW = groupW * BAR_WIDTH_FRACTION

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(weekData) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val idx = (offset.x / groupW)
                                        .toInt()
                                        .coerceIn(weekData.indices)
                                    pressedIdx = idx
                                    tooltipX = offset.x
                                    tryAwaitRelease()
                                    pressedIdx = null
                                },
                                onLongPress = { offset ->
                                    val idx = (offset.x / groupW)
                                        .toInt()
                                        .coerceIn(weekData.indices)
                                    pressedIdx = idx
                                    tooltipX = offset.x
                                },
                            )
                        },
                ) {
                    // ── Subtle horizontal guide lines ─────────────────────────
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    for (i in 1..GUIDE_LINES) {
                        val y = barsAreaH * (1f - i.toFloat() / GUIDE_LINES)
                        drawLine(
                            color = guideColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect,
                        )
                    }

                    // ── Stacked bars ──────────────────────────────────────────
                    weekData.forEachIndexed { wIdx, week ->
                        val scale = scales[wIdx]
                        val cx = wIdx * groupW + groupW / 2f  // group centre x
                        val left = cx - barW * scale / 2f
                        val right = cx + barW * scale / 2f
                        val actualBarW = right - left

                        var stackBottom = barsAreaH
                        // Work bottom-to-top but we want bottom segments drawn first
                        val segments = categories.mapIndexed { cIdx, cat ->
                            val amount = (week.categoryAmounts[cat] ?: 0.0).toFloat()
                            Triple(cIdx, cat, amount)
                        }.filter { it.third > 0f }

                        segments.forEachIndexed { segPos, (cIdx, _, amount) ->
                            val segH = (amount / maxTotal) * barsAreaH * scale
                            val segTop = stackBottom - segH
                            val isTopmost = segPos == segments.lastIndex
                            val color = seriesColors.getOrElse(cIdx) { seriesColors.last() }

                            drawStackedBarSegment(
                                left = left,
                                top = segTop,
                                width = actualBarW,
                                height = segH,
                                color = color,
                                roundTopCorners = isTopmost,
                                topRadiusPx = TOP_RADIUS_DP.dp.toPx(),
                            )
                            stackBottom = segTop
                        }

                        // ── Week label ────────────────────────────────────────
                        val label = week.label
                        val labelStyle = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (pressedIdx == wIdx) labelColor.copy(alpha = 1f)
                            else labelColor.copy(alpha = 0.72f),
                        )
                        val measured = textMeasurer.measure(label, labelStyle)
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(
                                x = cx - measured.size.width / 2f,
                                y = barsAreaH + (labelRowPx - measured.size.height) / 2f,
                            ),
                        )
                    }
                }

                // ── Tooltip pill ──────────────────────────────────────────────
                // Shown on press/hold; floats just above the pressed bar group,
                // nudged inward so it never clips outside the card edges.
                val pressed = pressedIdx
                androidx.compose.animation.AnimatedVisibility(
                    visible = pressed != null,
                    enter = fadeIn() + scaleIn(
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        initialScale = 0.82f,
                    ),
                    exit = fadeOut() + scaleOut(targetScale = 0.88f),
                ) {
                    if (pressed != null) {
                        val week = weekData.getOrNull(pressed)
                        val total = week?.let { w ->
                            categories.sumOf { c -> w.categoryAmounts[c] ?: 0.0 }
                        } ?: 0.0

                        // Convert px position to dp for Modifier.offset
                        val groupCxDp = with(density) {
                            ((pressed * groupW + groupW / 2f)).toDp()
                        }
                        val tooltipWidthDp = 96.dp
                        val chartWidthDp = with(density) { chartWidthPx.toDp() }

                        // Clamp so the tooltip stays inside the card
                        val rawLeft = groupCxDp - tooltipWidthDp / 2
                        val clampedLeft = rawLeft.coerceIn(0.dp, chartWidthDp - tooltipWidthDp)

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(x = clampedLeft.roundToPx(), y = 0) }
                                .width(tooltipWidthDp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(tooltipSurface)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = week?.label ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tooltipText.copy(alpha = 0.7f),
                                )
                                Text(
                                    text = "KES ${"%,.0f".format(total)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = tooltipText,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws a single stacked-bar segment with a vertical gradient (solid at top →
 * semi-transparent at bottom) that gives bars a sense of depth.  Only the
 * topmost segment gets rounded upper corners so the whole stack reads as one
 * continuous column.
 */
private fun DrawScope.drawStackedBarSegment(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    color: Color,
    roundTopCorners: Boolean,
    topRadiusPx: Float,
) {
    if (height <= 0f) return

    val gradient = Brush.verticalGradient(
        colors = listOf(color, color.copy(alpha = 0.55f)),
        startY = top,
        endY = top + height,
    )

    if (roundTopCorners && height > topRadiusPx) {
        // Draw the rounded-corner rect for the full segment…
        drawRoundRect(
            brush = gradient,
            topLeft = Offset(left, top),
            size = Size(width, height),
            cornerRadius = CornerRadius(topRadiusPx),
        )
        // …then fill over the bottom half of the corners to square them off,
        // so adjacent segments stack flush without visible gaps.
        val squareFillH = topRadiusPx
        drawRect(
            brush = gradient,
            topLeft = Offset(left, top + height - squareFillH),
            size = Size(width, squareFillH),
        )
    } else {
        // Bottom segment (no rounded corners) — plain rect
        drawRect(
            brush = gradient,
            topLeft = Offset(left, top),
            size = Size(width, height),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Monthly Spend Trend
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonthlySpendTrendSection(months: List<MonthlySpendData>) {
    if (months.isEmpty()) return

    val maxSpend = months.maxOfOrNull { it.totalSpend }?.coerceAtLeast(1.0) ?: 1.0

    AppCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Monthly Spending Trend",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            months.forEach { month ->
                MonthlySpendRow(month = month, maxSpend = maxSpend)
            }
        }
    }
}

@Composable
private fun MonthlySpendRow(month: MonthlySpendData, maxSpend: Double) {
    val ratio = (month.totalSpend / maxSpend).toFloat().coerceIn(0f, 1f)
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = androidx.compose.animation.core.tween(600),
        label = "monthly_bar_${month.label}",
    )
    val delta = month.totalSpend - month.previousTotal
    val deltaColor = when {
        month.previousTotal == 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
        delta > 0 -> MaterialTheme.colorScheme.error
        else -> com.personal.lifeOS.ui.theme.Success
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = month.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (month.previousTotal > 0.0 && delta != 0.0) {
                    Text(
                        text = "${if (delta > 0) "+" else ""}${com.personal.lifeOS.core.utils.DateUtils.formatCurrency(delta)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = deltaColor,
                    )
                }
                Text(
                    text = com.personal.lifeOS.core.utils.DateUtils.formatCurrency(month.totalSpend),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedRatio)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Insight cards
// ─────────────────────────────────────────────────────────────────────────────

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
                            imageVector = Icons.Outlined.AutoAwesome,
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
