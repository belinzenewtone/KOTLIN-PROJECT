@file:Suppress("LongMethod", "CyclomaticComplexMethod", "TooManyFunctions")

package com.personal.lifeOS.features.calendar.presentation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.calendar.domain.model.CalendarEvent
import com.personal.lifeOS.features.calendar.domain.model.EventImportance
import com.personal.lifeOS.features.calendar.domain.model.EventStatus
import com.personal.lifeOS.features.calendar.domain.model.EventType
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.Accent
import com.personal.lifeOS.ui.theme.BackgroundDark
import com.personal.lifeOS.ui.theme.CategoryOther
import com.personal.lifeOS.ui.theme.Error
import com.personal.lifeOS.ui.theme.GlassBorder
import com.personal.lifeOS.ui.theme.GlassWhite
import com.personal.lifeOS.ui.theme.Info
import com.personal.lifeOS.ui.theme.Primary
import com.personal.lifeOS.ui.theme.Success
import com.personal.lifeOS.ui.theme.SurfaceDark
import com.personal.lifeOS.ui.theme.TextPrimary
import com.personal.lifeOS.ui.theme.TextSecondary
import com.personal.lifeOS.ui.theme.TextTertiary
import com.personal.lifeOS.ui.theme.Warning
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

@Composable
internal fun CalendarMonthCard(
    state: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous", tint = TextPrimary)
                }
                Text(
                    text = "${state.currentMonth.month.getDisplayName(
                        TextStyle.FULL,
                        Locale.ENGLISH,
                    )} ${state.currentMonth.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next", tint = TextPrimary)
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
                        color = TextTertiary,
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
internal fun SelectedDateHeader(selectedDate: LocalDate) {
    Text(
        text =
            DateUtils.formatDate(
                selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                "EEEE, MMMM dd",
            ),
        style = MaterialTheme.typography.titleMedium,
        color = TextSecondary,
    )
}

@Composable
internal fun EmptyDayEventsCard() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "No events on this day",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
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
    val zone = ZoneId.systemDefault()

    val eventDays =
        state.events.map { event ->
            Instant.ofEpochMilli(event.date).atZone(zone).toLocalDate().dayOfMonth
        }.toSet()

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
                                            isSelected -> Primary
                                            isToday -> Primary.copy(alpha = 0.2f)
                                            else -> Color.Transparent
                                        },
                                    )
                                    .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$dayNumber",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color =
                                        when {
                                            isSelected -> BackgroundDark
                                            isToday -> Primary
                                            else -> TextPrimary
                                        },
                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                )
                                if (hasEvent) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) BackgroundDark else Accent),
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

@Composable
internal fun CalendarEventCard(
    event: CalendarEvent,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val typeColor =
        when (event.type) {
            EventType.WORK -> Info
            EventType.HEALTH -> Success
            EventType.FINANCE -> Warning
            EventType.PERSONAL -> Primary
            EventType.OTHER -> CategoryOther
        }
    val dismissState =
        rememberSwipeToDismissBoxState(
            positionalThreshold = { totalDistance -> totalDistance * 0.4f },
            confirmValueChange = { target ->
                when (target) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        if (event.status == EventStatus.PENDING) {
                            onComplete()
                            true
                        } else {
                            false
                        }
                    }

                    SwipeToDismissBoxValue.EndToStart -> {
                        onDelete()
                        true
                    }

                    else -> false
                }
            },
        )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val backgroundColor by animateColorAsState(
                targetValue =
                    when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> Success.copy(alpha = 0.3f)
                        SwipeToDismissBoxValue.EndToStart -> Error.copy(alpha = 0.3f)
                        else -> Color.Transparent
                    },
                label = "calendar-event-swipe-color",
            )
            val icon =
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Icons.Filled.CheckCircle
                } else {
                    Icons.Filled.Delete
                }
            val iconAlignment =
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(backgroundColor)
                        .padding(horizontal = 20.dp),
                contentAlignment = iconAlignment,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .width(4.dp)
                            .height(56.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (event.status == EventStatus.COMPLETED) TextTertiary else typeColor),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration =
                            if (event.status == EventStatus.COMPLETED) {
                                TextDecoration.LineThrough
                            } else {
                                null
                            },
                        color = if (event.status == EventStatus.COMPLETED) TextTertiary else TextPrimary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EventBadge(text = event.type.label, color = typeColor)
                        EventBadge(text = event.importance.label, color = importanceColor(event.importance))
                        if (event.status == EventStatus.COMPLETED) {
                            EventBadge(text = "Completed", color = Success)
                        }
                    }
                    Text(
                        text = "Time: ${DateUtils.formatDate(event.date, "MMM dd, yyyy h:mm a")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    if (event.endDate != null) {
                        Text(
                            text = "Due: ${DateUtils.formatDate(event.endDate, "MMM dd, yyyy h:mm a")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                    if (event.description.isNotBlank()) {
                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit event",
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EventBadge(
    text: String,
    color: Color,
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.18f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun importanceColor(importance: EventImportance): Color =
    when (importance) {
        EventImportance.URGENT -> Color(0xFFEF5350)
        EventImportance.IMPORTANT -> Color(0xFFFFB74D)
        EventImportance.NEUTRAL -> Color(0xFF42A5F5)
    }

@Composable
internal fun AddEventDialog(
    initialEvent: CalendarEvent?,
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (String, String, EventType, EventImportance, Long, Long?) -> Unit,
) {
    val isEdit = initialEvent != null
    var title by remember(initialEvent?.id) { mutableStateOf(initialEvent?.title.orEmpty()) }
    var description by remember(initialEvent?.id) { mutableStateOf(initialEvent?.description.orEmpty()) }
    var selectedType by remember(initialEvent?.id) { mutableStateOf(initialEvent?.type ?: EventType.PERSONAL) }
    var selectedImportance by
        remember(initialEvent?.id) {
            mutableStateOf(initialEvent?.importance ?: EventImportance.NEUTRAL)
        }
    var eventDateTime by
        remember(initialEvent?.id) {
            mutableLongStateOf(initialEvent?.date ?: defaultEventDateTime(selectedDate))
        }
    var dueDateTime by remember(initialEvent?.id) { mutableStateOf<Long?>(initialEvent?.endDate) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(24.dp),
        title = { Text(if (isEdit) "Edit Event" else "New Event", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = GlassWhite,
                            unfocusedContainerColor = GlassWhite,
                            cursorColor = Primary,
                        ),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = GlassWhite,
                            unfocusedContainerColor = GlassWhite,
                            cursorColor = Primary,
                        ),
                )

                Text("Type", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    EventType.entries.forEach { type ->
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (type == selectedType) Primary else GlassWhite)
                                    .clickable { selectedType = type }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = type.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (type == selectedType) BackgroundDark else TextSecondary,
                            )
                        }
                    }
                }

                Text("Priority", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventImportance.entries.forEach { importance ->
                        val selected = importance == selectedImportance
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) {
                                            importanceColor(importance)
                                        } else {
                                            importanceColor(importance).copy(alpha = 0.15f)
                                        },
                                    )
                                    .clickable { selectedImportance = importance }
                                    .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = importance.label,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = if (selected) BackgroundDark else importanceColor(importance),
                            )
                        }
                    }
                }

                DateTimePickerRow(
                    title = "Event Date",
                    icon = Icons.Filled.CalendarMonth,
                    value = DateUtils.formatDate(eventDateTime, "MMM dd, yyyy"),
                    onClick = {
                        val calendar = Calendar.getInstance().apply { timeInMillis = eventDateTime }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                eventDateTime =
                                    eventDateTime
                                        .withDate(
                                            year = year,
                                            month = month,
                                            day = day,
                                        )
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH),
                        ).show()
                    },
                )

                DateTimePickerRow(
                    title = "Event Time",
                    icon = Icons.Filled.Schedule,
                    value = DateUtils.formatTime(eventDateTime),
                    onClick = {
                        val calendar = Calendar.getInstance().apply { timeInMillis = eventDateTime }
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                eventDateTime =
                                    eventDateTime.withTime(
                                        hour = hour,
                                        minute = minute,
                                    )
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false,
                        ).show()
                    },
                )

                DateTimePickerRow(
                    title = "Due Date (optional)",
                    icon = Icons.Filled.CalendarMonth,
                    value = dueDateTime?.let { DateUtils.formatDate(it, "MMM dd, yyyy") } ?: "Set due date",
                    onClick = {
                        val base = dueDateTime ?: eventDateTime
                        val calendar = Calendar.getInstance().apply { timeInMillis = base }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val current = dueDateTime ?: eventDateTime.withTime(hour = 23, minute = 59)
                                dueDateTime =
                                    current.withDate(
                                        year = year,
                                        month = month,
                                        day = day,
                                    )
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH),
                        ).show()
                    },
                )

                DateTimePickerRow(
                    title = "Due Time (optional)",
                    icon = Icons.Filled.Schedule,
                    value = dueDateTime?.let { DateUtils.formatTime(it) } ?: "Set due time",
                    onClick = {
                        val base = dueDateTime ?: eventDateTime.withTime(hour = 23, minute = 59)
                        val calendar = Calendar.getInstance().apply { timeInMillis = base }
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                val current = dueDateTime ?: eventDateTime.withTime(hour = 23, minute = 59)
                                dueDateTime =
                                    current.withTime(
                                        hour = hour,
                                        minute = minute,
                                    )
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false,
                        ).show()
                    },
                )

                if (dueDateTime != null) {
                    TextButton(onClick = { dueDateTime = null }) {
                        Text("Clear due date/time", color = Error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            title,
                            description,
                            selectedType,
                            selectedImportance,
                            eventDateTime,
                            dueDateTime,
                        )
                    }
                },
            ) {
                Text(if (isEdit) "Save" else "Add", color = Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
    )
}

@Composable
private fun DateTimePickerRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GlassWhite)
                .clickable(onClick = onClick)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
            Text(
                text = value,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun defaultEventDateTime(selectedDate: LocalDate): Long {
    return Calendar.getInstance().apply {
        set(selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth, 9, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun Long.withDate(
    year: Int,
    month: Int,
    day: Int,
): Long {
    return Calendar.getInstance().apply {
        timeInMillis = this@withDate
        set(year, month, day)
    }.timeInMillis
}

private fun Long.withTime(
    hour: Int,
    minute: Int,
): Long {
    return Calendar.getInstance().apply {
        timeInMillis = this@withTime
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
