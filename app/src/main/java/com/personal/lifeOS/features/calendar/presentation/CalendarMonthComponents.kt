package com.personal.lifeOS.features.calendar.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.features.calendar.domain.model.CalendarEvent
import com.personal.lifeOS.features.calendar.domain.model.EventType
import com.personal.lifeOS.ui.theme.CategoryOther
import com.personal.lifeOS.ui.theme.Info
import com.personal.lifeOS.ui.theme.Primary
import com.personal.lifeOS.ui.theme.Success
import com.personal.lifeOS.ui.theme.Warning
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

// ── Event type colour palette ────────────────────────────────────────────────
private val EventType.dotColor: Color
    get() = when (this) {
        EventType.WORK -> Info
        EventType.FINANCE -> Warning
        EventType.HEALTH -> Success
        EventType.PERSONAL -> Primary
        EventType.OTHER -> CategoryOther
    }

@Composable
internal fun CalendarMonthCard(
    state: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToToday: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val monthLabel = state.currentMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            // Swipe left → next month, swipe right → previous month
            .pointerInput(Unit) {
                var accumulated = 0f
                detectHorizontalDragGestures(
                    onDragStart = { accumulated = 0f },
                    onDragEnd = { accumulated = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        accumulated += dragAmount
                        change.consume()
                        when {
                            accumulated > 80f -> { accumulated = 0f; onPreviousMonth() }
                            accumulated < -80f -> { accumulated = 0f; onNextMonth() }
                        }
                    },
                )
            },
        elevated = true,
    ) {
        Column {
            // ── Month header row ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "Previous month",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$monthLabel ${state.currentMonth.year}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    // "Today" jump button — visible only when browsing away from the current month
                    if (!state.isViewingCurrentMonth) {
                        TextButton(
                            onClick = onGoToToday,
                        ) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                IconButton(onClick = onNextMonth) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "Next month",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Day-of-week header ───────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Animated calendar grid ───────────────────────────────────────
            AnimatedContent(
                targetState = state.currentMonth,
                transitionSpec = {
                    if (targetState > initialState) {
                        // Navigating forward → slide in from the right
                        slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                    } else {
                        // Navigating backward → slide in from the left
                        slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "CalendarMonthGrid",
            ) { targetMonth ->
                CalendarGrid(
                    month = targetMonth,
                    selectedDate = state.selectedDate,
                    events = state.events,
                    onDateSelected = onDateSelected,
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    events: List<CalendarEvent>,
    onDateSelected: (LocalDate) -> Unit,
) {
    val firstDay = month.atDay(1)
    val startOffset = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val daysInMonth = month.lengthOfMonth()
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()

    // Map each day-of-month to the distinct event-type colours present on that day
    val dayColorMap: Map<Int, List<Color>> = events.toDayColorMap(zone)

    Column {
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - startOffset + 1

                    if (dayNumber in 1..daysInMonth) {
                        val date = month.atDay(dayNumber)
                        val isSelected = date == selectedDate
                        val isToday = date == today
                        val dotColors = dayColorMap[dayNumber].orEmpty()

                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            else -> Color.Transparent
                                        },
                                    ).clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$dayNumber",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color =
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                )
                                // Event type colour dots — up to 3, one per distinct type
                                if (dotColors.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        dotColors.take(3).forEach { color ->
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                        else color,
                                                    ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

/**
 * Returns a map of day-of-month → distinct event-type colours on that day.
 * At most one colour per EventType so the dot row stays compact.
 */
private fun List<CalendarEvent>.toDayColorMap(zoneId: ZoneId): Map<Int, List<Color>> {
    val result = mutableMapOf<Int, MutableSet<Color>>()
    forEach { event ->
        val day = Instant.ofEpochMilli(event.date).atZone(zoneId).toLocalDate().dayOfMonth
        result.getOrPut(day) { mutableSetOf() }.add(event.type.dotColor)
    }
    return result.mapValues { (_, set) -> set.toList() }
}
