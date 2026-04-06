@file:Suppress("LongMethod")

package com.personal.lifeOS.features.calendar.presentation

import androidx.compose.foundation.layout.Arrangement
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
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.calendar.domain.model.CalendarEvent
import com.personal.lifeOS.features.calendar.domain.model.EventImportance
import com.personal.lifeOS.features.calendar.domain.model.EventType
import com.personal.lifeOS.ui.theme.AppSpacing

@Composable
fun EventsScreen(
    onBack: (() -> Unit)? = null,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<CalendarEvent?>(null) }

    // Show all events from now onwards, sorted chronologically.
    val nowMs = System.currentTimeMillis()
    val upcomingEvents = remember(state.events, query) {
        state.events
            .filter { it.date >= nowMs || it.endDate?.let { end -> end >= nowMs } == true }
            .sortedBy { it.date }
            .filterByQuery(query)
    }

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
                Icon(Icons.Outlined.Add, contentDescription = "Add event")
            }
        },
    ) { innerPadding ->
        PageScaffold(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            headerEyebrow = "Schedule",
            title = "Events",
            subtitle = "Upcoming",
            onBack = onBack,
            contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFab),
        ) {
            state.error?.let {
                InlineBanner(message = it, tone = InlineBannerTone.ERROR)
            }

            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search events...",
            )

            if (upcomingEvents.isEmpty()) {
                EmptyState(
                    title = "No upcoming events",
                    description = "Tap + to schedule your next event.",
                )
            } else {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    upcomingEvents.forEach { event ->
                        CalendarEventChip(
                            title = event.title,
                            timeLabel = DateUtils.formatDate(event.date, "EEE, MMM dd - h:mm a"),
                        )
                        CalendarEventCard(
                            event = event,
                            onComplete = { viewModel.markEventCompleted(event) },
                            onEdit = { viewModel.showEditDialog(event) },
                            onDelete = { deleteTarget = event },
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation.
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
            onSaveTask = { title, desc, priority, deadline ->
                viewModel.saveTask(title, desc, priority, deadline)
            },
            onSaveEvent = { title, desc, type, importance, startAt, endAt, hasReminder, reminderMins ->
                viewModel.saveEvent(title, desc, type, importance, startAt, endAt, hasReminder, reminderMins)
            },
        )
    }
}

private fun List<CalendarEvent>.filterByQuery(query: String): List<CalendarEvent> {
    if (query.isBlank()) return this
    val search = query.trim()
    return filter { event ->
        event.title.contains(search, ignoreCase = true) ||
            event.description.contains(search, ignoreCase = true)
    }
}
