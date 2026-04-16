package com.personal.lifeOS.features.calendar.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.CalendarEventChip
import com.personal.lifeOS.core.ui.designsystem.EmptyState
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.core.ui.designsystem.SearchField
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.calendar.domain.model.CalendarEvent
import com.personal.lifeOS.features.calendar.domain.model.EventImportance
import com.personal.lifeOS.features.calendar.domain.model.EventKind
import com.personal.lifeOS.features.calendar.domain.model.EventType
import com.personal.lifeOS.features.calendar.domain.model.RepeatRule
import com.personal.lifeOS.features.tasks.domain.model.TaskPriority
import com.personal.lifeOS.ui.theme.AppSpacing

@Composable
@Suppress("LongMethod")
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<CalendarEvent?>(null) }
    val events = remember(state.selectedDayEvents, query) { state.selectedDayEvents.filterEventsByQuery(query) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                FloatingActionButton(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = AppSpacing.FabBottomOffset),
                    onClick = { viewModel.showAddDialog() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add")
                }
            },
        ) { padding ->
            CalendarBody(
                state = state,
                events = events,
                query = query,
                padding = padding,
                onQueryChange = { query = it },
                onNavigateMonth = { offset -> viewModel.navigateMonth(offset) },
                onGoToToday = { viewModel.goToToday() },
                onSelectDate = { selected -> viewModel.selectDate(selected) },
                onComplete = { event -> viewModel.markEventCompleted(event) },
                onEdit = { event -> viewModel.showEditDialog(event) },
                onDelete = { event -> deleteTarget = event },
            )
        }

        // ── Full-page add / edit screen ────────────────────────────────────────
        CalendarAddScreenOverlay(
            visible = state.showAddScreen,
            editingEvent = state.editingEvent,
            selectedDate = state.selectedDate,
            onDismiss = { viewModel.hideAddDialog() },
            onSaveTask = { title, desc, priority, deadline ->
                viewModel.saveTask(title, desc, priority, deadline)
            },
            onSaveEvent = { title, desc, type, importance, date, endDate, allDay, repeatRule,
                            reminderOffsets, alarmEnabled, guests, timeZoneId, kind ->
                viewModel.saveEvent(
                    title = title, description = desc, type = type, importance = importance,
                    date = date, endDate = endDate, allDay = allDay, repeatRule = repeatRule,
                    reminderOffsets = reminderOffsets, alarmEnabled = alarmEnabled,
                    guests = guests, timeZoneId = timeZoneId, kind = kind,
                )
            },
        )
    }

    // ── Delete confirmation dialog ─────────────────────────────────────────────
    deleteTarget?.let { event ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete event?") },
            text = { Text("Remove \"${event.title}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEvent(event)
                    deleteTarget = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
@Suppress("LongParameterList", "LongMethod")
private fun CalendarBody(
    state: CalendarUiState,
    events: List<CalendarEvent>,
    query: String,
    padding: PaddingValues,
    onQueryChange: (String) -> Unit,
    onNavigateMonth: (Int) -> Unit,
    onGoToToday: () -> Unit,
    onSelectDate: (java.time.LocalDate) -> Unit,
    onComplete: (CalendarEvent) -> Unit,
    onEdit: (CalendarEvent) -> Unit,
    onDelete: (CalendarEvent) -> Unit,
) {
    PageScaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        headerEyebrow = "Schedule",
        title = "Calendar",
        subtitle = DateUtils.formatDate(System.currentTimeMillis(), "MMMM yyyy"),
        contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFab),
    ) {
        state.error?.let {
            InlineBanner(message = it, tone = InlineBannerTone.ERROR)
        }

        CalendarMonthCard(
            state = state,
            onPreviousMonth = { onNavigateMonth(-1) },
            onNextMonth = { onNavigateMonth(1) },
            onGoToToday = onGoToToday,
            onDateSelected = onSelectDate,
        )

        Text(
            text = DateUtils.formatDate(
                state.selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                "EEEE, MMM dd",
            ),
            style = MaterialTheme.typography.titleMedium,
        )

        SearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "Search events for selected day",
        )

        if (events.isEmpty()) {
            EmptyState(
                title = "No events on this day",
                description = "Tap + to add an event, birthday, countdown and more.",
            )
        } else {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                events.forEach { event ->
                    CalendarEventChip(
                        title = event.title,
                        timeLabel = if (event.allDay) "All day" else DateUtils.formatTime(event.date),
                    )
                    CalendarEventCard(
                        event = event,
                        onComplete = { onComplete(event) },
                        onEdit = { onEdit(event) },
                        onDelete = { onDelete(event) },
                    )
                }
            }
        }
    }
}

private fun List<CalendarEvent>.filterEventsByQuery(query: String): List<CalendarEvent> {
    if (query.isBlank()) return this
    val search = query.trim()
    return filter { event ->
        event.title.contains(search, ignoreCase = true) ||
            event.description.contains(search, ignoreCase = true)
    }
}
