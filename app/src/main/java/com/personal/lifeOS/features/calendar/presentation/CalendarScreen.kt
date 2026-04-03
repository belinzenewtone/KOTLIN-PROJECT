package com.personal.lifeOS.features.calendar.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.CalendarEventChip
import com.personal.lifeOS.core.ui.designsystem.EmptyState
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.core.ui.designsystem.SearchField
import com.personal.lifeOS.core.ui.designsystem.SuperAddBottomSheet
import com.personal.lifeOS.core.ui.designsystem.SuperKind
import com.personal.lifeOS.core.ui.designsystem.TaskRow
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.calendar.domain.model.CalendarEvent
import com.personal.lifeOS.features.calendar.domain.model.EventImportance
import com.personal.lifeOS.features.calendar.domain.model.EventType
import com.personal.lifeOS.features.tasks.domain.model.Task
import com.personal.lifeOS.features.tasks.domain.model.TaskStatus
import com.personal.lifeOS.ui.theme.AppSpacing

@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<com.personal.lifeOS.features.calendar.domain.model.CalendarEvent?>(null) }
    val events = remember(state.selectedDayEvents, query) { state.selectedDayEvents.filterEventsByQuery(query) }
    val tasks = remember(state.selectedDayTasks, query) { state.selectedDayTasks.filterTasksByQuery(query) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                modifier =
                    Modifier
                        .navigationBarsPadding()
                        .padding(bottom = AppSpacing.FabBottomOffset),
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add event")
            }
        },
    ) { padding ->
        CalendarBody(
            state = state,
            events = events,
            tasks = tasks,
            query = query,
            padding = padding,
            onQueryChange = { query = it },
            onNavigateMonth = { offset -> viewModel.navigateMonth(offset) },
            onGoToToday = { viewModel.goToToday() },
            onSelectDate = { selected -> viewModel.selectDate(selected) },
            onComplete = { event -> viewModel.markEventCompleted(event) },
            onEdit = { event -> viewModel.showEditDialog(event) },
            onDelete = { event -> deleteTarget = event },
            onCompleteTask = { task -> viewModel.completeTask(task) },
            onUndoTask = { task -> viewModel.undoCompleteTask(task) },
        )
    }

    // ── Delete confirmation dialog ─────────────────────────────────────────
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

    if (state.showAddDialog) {
        SuperAddBottomSheet(
            defaultKind = SuperKind.EVENT,
            selectedDate = state.selectedDate,
            isEdit = state.editingEvent != null,
            editTitle = state.editingEvent?.title.orEmpty(),
            editDescription = state.editingEvent?.description.orEmpty(),
            editEventType = state.editingEvent?.type ?: EventType.PERSONAL,
            editImportance = state.editingEvent?.importance ?: EventImportance.NEUTRAL,
            editStartAt = state.editingEvent?.date,
            editEndAt = state.editingEvent?.endDate,
            editHasReminder = state.editingEvent?.hasReminder ?: false,
            editReminderMinutes = state.editingEvent?.reminderMinutesBefore ?: 15,
            onDismiss = { viewModel.hideAddDialog() },
            onSaveTask = { _, _, _, _ ->
                // Calendar screen never saves tasks — no-op
            },
            onSaveEvent = { title, desc, type, importance, startAt, endAt, hasReminder, reminderMins ->
                viewModel.saveEvent(title, desc, type, importance, startAt, endAt, hasReminder, reminderMins)
            },
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun CalendarBody(
    state: CalendarUiState,
    events: List<CalendarEvent>,
    tasks: List<Task>,
    query: String,
    padding: PaddingValues,
    onQueryChange: (String) -> Unit,
    onNavigateMonth: (Int) -> Unit,
    onGoToToday: () -> Unit,
    onSelectDate: (java.time.LocalDate) -> Unit,
    onComplete: (CalendarEvent) -> Unit,
    onEdit: (CalendarEvent) -> Unit,
    onDelete: (CalendarEvent) -> Unit,
    onCompleteTask: (Task) -> Unit,
    onUndoTask: (Task) -> Unit,
) {
    PageScaffold(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding),
        headerEyebrow = "Schedule",
        title = "Calendar",
        subtitle = DateUtils.formatDate(System.currentTimeMillis(), "MMMM yyyy"),
        contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFab),
    ) {
        state.error?.let {
            InlineBanner(
                message = it,
                tone = InlineBannerTone.ERROR,
            )
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
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        )

        SearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "Search tasks and events for selected day",
        )

        Text(
            text = "Events",
            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (events.isEmpty()) {
            EmptyState(
                title = "No events on this day",
                description = "Tap + to add an event or pick another date.",
            )
        } else {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                events.forEach { event ->
                    CalendarEventChip(
                        title = event.title,
                        timeLabel = DateUtils.formatTime(event.date),
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

        Text(
            text = "Tasks",
            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (tasks.isEmpty()) {
            EmptyState(
                title = "No tasks due on this day",
                description = "Add a deadline to a task and it will appear here.",
            )
        } else {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                tasks.forEach { task ->
                    val completed = task.status == TaskStatus.COMPLETED
                    TaskRow(
                        title = task.title,
                        subtitle = task.calendarSubtitle(),
                        isCompleted = completed,
                        priority = if (completed) "" else task.priority.name,
                        onToggleComplete = {
                            if (completed) onUndoTask(task) else onCompleteTask(task)
                        },
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

private fun List<Task>.filterTasksByQuery(query: String): List<Task> {
    if (query.isBlank()) return this
    val search = query.trim()
    return filter { task ->
        task.title.contains(search, ignoreCase = true) ||
            task.description.contains(search, ignoreCase = true)
    }
}

private fun Task.calendarSubtitle(): String {
    return when {
        description.isNotBlank() -> description
        deadline != null -> "Due ${DateUtils.formatDate(deadline, "h:mm a")}"
        else -> "No description"
    }
}
