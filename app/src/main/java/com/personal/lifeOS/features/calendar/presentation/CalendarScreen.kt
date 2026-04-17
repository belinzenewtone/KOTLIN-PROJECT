package com.personal.lifeOS.features.calendar.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
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
import com.personal.lifeOS.ui.theme.CategoryAnniversary
import com.personal.lifeOS.ui.theme.CategoryBirthday
import com.personal.lifeOS.ui.theme.CategoryCountdown
import com.personal.lifeOS.ui.theme.Info

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
                            reminderOffsets, alarmEnabled, guests, timeZoneId, kind, reminderTimeOfDayMinutes ->
                viewModel.saveEvent(
                    title = title, description = desc, type = type, importance = importance,
                    date = date, endDate = endDate, allDay = allDay, repeatRule = repeatRule,
                    reminderOffsets = reminderOffsets, alarmEnabled = alarmEnabled,
                    guests = guests, timeZoneId = timeZoneId, kind = kind,
                    reminderTimeOfDayMinutes = reminderTimeOfDayMinutes,
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
            placeholder = "Search across all categories",
        )

        if (events.isEmpty()) {
            EmptyState(
                title = "Nothing for the day",
                description = "Tap + to add an event, birthday, countdown and more.",
            )
        } else {
            DayViewContent(
                events = events,
                onComplete = onComplete,
                onEdit = onEdit,
                onDelete = onDelete,
            )
        }
    }
}

// ── Issue 9: Categorised day view ─────────────────────────────────────────────

@Composable
private fun DayViewContent(
    events: List<CalendarEvent>,
    onComplete: (CalendarEvent) -> Unit,
    onEdit: (CalendarEvent) -> Unit,
    onDelete: (CalendarEvent) -> Unit,
) {
    // Group by EventKind, then sort each group chronologically
    val kindOrder = listOf(EventKind.EVENT, EventKind.BIRTHDAY, EventKind.ANNIVERSARY, EventKind.COUNTDOWN)
    val grouped = events
        .sortedBy { it.date }
        .groupBy { it.kind }
    val presentKinds = kindOrder.filter { grouped.containsKey(it) }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        presentKinds.forEachIndexed { sectionIdx, kind ->
            val sectionEvents = grouped[kind] ?: return@forEachIndexed
            val sectionColor = when (kind) {
                EventKind.EVENT -> Info
                EventKind.BIRTHDAY -> CategoryBirthday
                EventKind.ANNIVERSARY -> CategoryAnniversary
                EventKind.COUNTDOWN -> CategoryCountdown
            }
            val sectionLabel = when (kind) {
                EventKind.EVENT -> "Events"
                EventKind.BIRTHDAY -> "Birthdays"
                EventKind.ANNIVERSARY -> "Anniversaries"
                EventKind.COUNTDOWN -> "Countdowns"
            }
            if (sectionIdx > 0) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = sectionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = sectionColor,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                sectionEvents.forEach { event ->
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
