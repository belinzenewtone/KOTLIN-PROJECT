package com.personal.lifeOS.features.calendar.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.calendar.domain.model.CalendarEvent
import com.personal.lifeOS.features.calendar.domain.model.EventType
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.*
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = Primary,
                contentColor = TextPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, "Add event")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Text("Calendar", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(8.dp))
            }

            // Month navigation
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.navigateMonth(-1) }) {
                                Icon(Icons.Filled.ChevronLeft, "Previous", tint = TextPrimary)
                            }
                            Text(
                                "${state.currentMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${state.currentMonth.year}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(onClick = { viewModel.navigateMonth(1) }) {
                                Icon(Icons.Filled.ChevronRight, "Next", tint = TextPrimary)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Day of week headers
                        Row(modifier = Modifier.fillMaxWidth()) {
                            val daysOfWeek = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                            daysOfWeek.forEach { day ->
                                Text(
                                    text = day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Calendar grid
                        CalendarGrid(
                            state = state,
                            onDateSelected = { viewModel.selectDate(it) }
                        )
                    }
                }
            }

            // Selected day events
            item {
                Text(
                    DateUtils.formatDate(
                        state.selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        "EEEE, MMMM dd"
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
            }

            if (state.selectedDayEvents.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "No events on this day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            items(state.selectedDayEvents, key = { it.id }) { event ->
                EventCard(event = event, onDelete = { viewModel.deleteEvent(event) })
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }

    if (state.showAddDialog) {
        AddEventDialog(
            onDismiss = { viewModel.hideAddDialog() },
            onAdd = { title, desc, type -> viewModel.addEvent(title, desc, type) }
        )
    }
}

@Composable
private fun CalendarGrid(
    state: CalendarUiState,
    onDateSelected: (LocalDate) -> Unit
) {
    val month = state.currentMonth
    val firstDay = month.atDay(1)
    val startOffset = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val daysInMonth = month.lengthOfMonth()
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()

    // Pre-compute which days have events
    val eventDays = state.events.map { event ->
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
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> Primary
                                        isToday -> Primary.copy(alpha = 0.2f)
                                        else -> androidx.compose.ui.graphics.Color.Transparent
                                    }
                                )
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$dayNumber",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        isSelected -> BackgroundDark
                                        isToday -> Primary
                                        else -> TextPrimary
                                    },
                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (hasEvent) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) BackgroundDark else Accent)
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
private fun EventCard(event: CalendarEvent, onDelete: () -> Unit) {
    val typeColor = when (event.type) {
        EventType.WORK -> Info
        EventType.HEALTH -> Success
        EventType.FINANCE -> Warning
        EventType.PERSONAL -> Primary
        EventType.OTHER -> CategoryOther
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(typeColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        event.type.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = typeColor
                    )
                    Text(
                        DateUtils.formatTime(event.date),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (event.description.isNotBlank()) {
                    Text(
                        event.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 2
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Delete", tint = Error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun AddEventDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, EventType) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(EventType.PERSONAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("New Event", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = GlassBorder
                    )
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = GlassBorder
                    )
                )
                Text("Type", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EventType.entries.forEach { type ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (type == selectedType) Primary else GlassWhite)
                                .clickable { selectedType = type }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                type.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (type == selectedType) BackgroundDark else TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onAdd(title, description, selectedType) }
            ) { Text("Add", color = Primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
