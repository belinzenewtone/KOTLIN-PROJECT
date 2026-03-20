package com.personal.lifeOS.features.calendar.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.features.calendar.domain.model.CalendarEvent
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun CalendarMonthCard(
    state: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val monthLabel =
        state.currentMonth.month.getDisplayName(
            TextStyle.FULL,
            Locale.ENGLISH,
        )

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        elevated = true,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous", tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    text = "$monthLabel ${state.currentMonth.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(Modifier.height(8.dp))

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

            CalendarGrid(
                state = state,
                onDateSelected = onDateSelected,
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    state: CalendarUiState,
    onDateSelected: (LocalDate) -> Unit,
) {
    val month = state.currentMonth
    val firstDay = month.atDay(1)
    val startOffset = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val daysInMonth = month.lengthOfMonth()
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7
    val today = LocalDate.now()
    val eventDays = state.events.toDaySet(ZoneId.systemDefault())

    Column {
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - startOffset + 1

                    if (dayNumber in 1..daysInMonth) {
                        val date = month.atDay(dayNumber)
                        val isSelected = date == state.selectedDate
                        val isToday = date == today
                        val hasEvent = dayNumber in eventDays

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
                                if (hasEvent) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary),
                                    )
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

private fun List<CalendarEvent>.toDaySet(zoneId: ZoneId): Set<Int> {
    return map { event ->
        Instant.ofEpochMilli(event.date).atZone(zoneId).toLocalDate().dayOfMonth
    }.toSet()
}
