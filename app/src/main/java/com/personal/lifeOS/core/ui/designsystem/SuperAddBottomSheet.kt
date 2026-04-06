@file:Suppress("LongMethod", "CyclomaticComplexMethod")

package com.personal.lifeOS.core.ui.designsystem

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.calendar.domain.model.EventImportance
import com.personal.lifeOS.features.calendar.domain.model.EventType
import com.personal.lifeOS.features.tasks.domain.model.TaskPriority
import com.personal.lifeOS.ui.theme.Error
import com.personal.lifeOS.ui.theme.Info
import com.personal.lifeOS.ui.theme.Warning
import java.time.LocalDate
import java.util.Calendar

/**
 * Determines which kind of planner item the sheet is creating or editing.
 * Used as the toggle state at the top of [SuperAddBottomSheet].
 */
enum class SuperKind(val label: String) {
    TASK("Task"),
    EVENT("Event"),
}

/**
 * A unified "add / edit" bottom sheet that handles both **Tasks** and **Calendar Events**
 * from a single, progressively-disclosed surface.
 *
 * The sheet starts in a compact state (kind toggle + title + when row) and reveals
 * the full details section (description, priority, event-specific fields) via an
 * expandable "Details" row — keeping the initial experience fast and uncluttered.
 *
 * Persistence is intentionally **not** done inside this composable: the caller
 * receives typed callbacks ([onSaveTask] / [onSaveEvent]) and is responsible for
 * passing them to the appropriate ViewModel / Repository.  This keeps the component
 * reusable and the architecture clean (Approach Alpha from the design doc).
 *
 * @param defaultKind    Which toggle is pre-selected when the sheet opens.
 * @param selectedDate   Context date; used to pre-fill event start time for a natural default.
 * @param isEdit         When true, the save button reads "Save" instead of "Create".
 * @param onDismiss      Called when the user cancels or swipes the sheet away.
 * @param onSaveTask     Called with (title, description, priority, deadline?) when saving a Task.
 * @param onSaveEvent    Called with (title, desc, type, importance, startAt, endAt?, hasReminder, reminderMins) when saving an Event.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAddBottomSheet(
    defaultKind: SuperKind = SuperKind.TASK,
    selectedDate: LocalDate = LocalDate.now(),
    // pre-populate when editing
    editTitle: String = "",
    editDescription: String = "",
    editPriority: TaskPriority = TaskPriority.NEUTRAL,
    editDeadline: Long? = null,
    editEventType: EventType = EventType.PERSONAL,
    editImportance: EventImportance = EventImportance.NEUTRAL,
    editStartAt: Long? = null,
    editEndAt: Long? = null,
    editHasReminder: Boolean = false,
    editReminderMinutes: Int = 15,
    isEdit: Boolean = false,
    onDismiss: () -> Unit,
    onSaveTask: (title: String, desc: String, priority: TaskPriority, deadline: Long?) -> Unit,
    onSaveEvent: (
        title: String,
        desc: String,
        type: EventType,
        importance: EventImportance,
        startAt: Long,
        endAt: Long?,
        hasReminder: Boolean,
        reminderMinutes: Int,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    // ── Form state ────────────────────────────────────────────────────────────
    var kind by remember { mutableStateOf(defaultKind) }
    var title by remember { mutableStateOf(editTitle) }
    var description by remember { mutableStateOf(editDescription) }
    var priority by remember { mutableStateOf(editPriority) }
    var deadline by remember { mutableStateOf(editDeadline) }
    var eventType by remember { mutableStateOf(editEventType) }
    var importance by remember { mutableStateOf(editImportance) }
    var startAt by remember {
        mutableLongStateOf(
            editStartAt ?: Calendar.getInstance().apply {
                set(
                    selectedDate.year,
                    selectedDate.monthValue - 1,
                    selectedDate.dayOfMonth,
                    9, 0, 0,
                )
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis,
        )
    }
    var endAt by remember { mutableStateOf(editEndAt) }
    var hasReminder by remember { mutableStateOf(editHasReminder) }
    var reminderMinutes by remember { mutableIntStateOf(editReminderMinutes) }
    var showDetails by remember { mutableStateOf(isEdit) }
    var titleError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

            // ── Kind toggle ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SuperKind.entries.forEach { k ->
                    val selected = k == kind
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(AppDesignTokens.radius.sm))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                            )
                            .clickable {
                                kind = k
                                titleError = false
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = k.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ── Title ──────────────────────────────────────────────────────
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError) titleError = false
                },
                label = { Text("Title") },
                singleLine = true,
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Title is required") }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDesignTokens.radius.md),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )

            // ── When ───────────────────────────────────────────────────────
            if (kind == SuperKind.TASK) {
                WhenSection(
                    rowLabel = "Deadline",
                    dateText = deadline?.let { DateUtils.formatDate(it, "MMM dd, yyyy") } ?: "No deadline",
                    timeText = deadline?.let { DateUtils.formatTime(it) } ?: "—",
                    hasValue = deadline != null,
                    onPickDate = {
                        val cal = Calendar.getInstance().also { c -> deadline?.let { c.timeInMillis = it } }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                deadline = Calendar.getInstance().apply {
                                    deadline?.let { timeInMillis = it } ?: run {
                                        set(Calendar.HOUR_OF_DAY, 23)
                                        set(Calendar.MINUTE, 59)
                                    }
                                    set(y, m, d)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH),
                        ).show()
                    },
                    onPickTime = {
                        val cal = Calendar.getInstance().also { c -> deadline?.let { c.timeInMillis = it } }
                        TimePickerDialog(
                            context,
                            { _, h, min ->
                                deadline = Calendar.getInstance().apply {
                                    deadline?.let { timeInMillis = it }
                                    set(Calendar.HOUR_OF_DAY, h)
                                    set(Calendar.MINUTE, min)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            false,
                        ).show()
                    },
                    onClear = { deadline = null },
                )
            } else {
                // Event: required start + optional end
                WhenSection(
                    rowLabel = "Starts",
                    dateText = DateUtils.formatDate(startAt, "MMM dd, yyyy"),
                    timeText = DateUtils.formatTime(startAt),
                    hasValue = true,
                    onPickDate = {
                        val cal = Calendar.getInstance().also { c -> c.timeInMillis = startAt }
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> startAt = startAt.withDate(y, m, d) },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH),
                        ).show()
                    },
                    onPickTime = {
                        val cal = Calendar.getInstance().also { c -> c.timeInMillis = startAt }
                        TimePickerDialog(
                            context,
                            { _, h, min -> startAt = startAt.withTime(h, min) },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            false,
                        ).show()
                    },
                    onClear = null, // start is required for events
                )

                WhenSection(
                    rowLabel = "Ends",
                    dateText = endAt?.let { DateUtils.formatDate(it, "MMM dd, yyyy") } ?: "Optional",
                    timeText = endAt?.let { DateUtils.formatTime(it) } ?: "—",
                    hasValue = endAt != null,
                    onPickDate = {
                        val base = endAt ?: startAt
                        val cal = Calendar.getInstance().also { c -> c.timeInMillis = base }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val current = endAt ?: startAt.withTime(23, 59)
                                endAt = current.withDate(y, m, d)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH),
                        ).show()
                    },
                    onPickTime = {
                        val base = endAt ?: startAt.withTime(23, 59)
                        val cal = Calendar.getInstance().also { c -> c.timeInMillis = base }
                        TimePickerDialog(
                            context,
                            { _, h, min ->
                                val current = endAt ?: startAt.withTime(23, 59)
                                endAt = current.withTime(h, min)
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            false,
                        ).show()
                    },
                    onClear = { endAt = null },
                )
            }

            // ── Details toggle ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppDesignTokens.radius.sm))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { showDetails = !showDetails }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (showDetails) "Hide details" else "More details",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = if (showDetails) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            // ── Details content ────────────────────────────────────────────
            AnimatedVisibility(
                visible = showDetails,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppDesignTokens.radius.md),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )

                    // Priority — shared between tasks and events, mapped to the right enum on save
                    val activePriority = if (kind == SuperKind.TASK) priority else importance.toTaskPriority()
                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TaskPriority.entries.forEach { p ->
                            val selected = p == activePriority
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(AppDesignTokens.radius.sm))
                                    .background(
                                        if (selected) superPriorityColor(p)
                                        else superPriorityColor(p).copy(alpha = 0.15f),
                                    )
                                    .clickable {
                                        if (kind == SuperKind.TASK) priority = p
                                        else importance = p.toEventImportance()
                                    }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = p.label,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else superPriorityColor(p),
                                )
                            }
                        }
                    }

                    // Event-only: Category chips
                    if (kind == SuperKind.EVENT) {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            EventType.entries.forEach { t ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(AppDesignTokens.radius.md))
                                        .background(
                                            if (t == eventType) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                        )
                                        .clickable { eventType = t }
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                ) {
                                    Text(
                                        text = t.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (t == eventType) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        // Reminder row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(AppDesignTokens.radius.sm))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text = "Reminder",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (hasReminder) {
                                    Text(
                                        text = "$reminderMinutes min before",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            LifeOSSwitch(checked = hasReminder, onCheckedChange = { hasReminder = it })
                        }

                        // Reminder time presets — only shown when reminder is on
                        AnimatedVisibility(visible = hasReminder) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                listOf(5, 10, 15, 30, 60).forEach { mins ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(AppDesignTokens.radius.md))
                                            .background(
                                                if (mins == reminderMinutes) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            )
                                            .clickable { reminderMinutes = mins }
                                            .padding(horizontal = 14.dp, vertical = 7.dp),
                                    ) {
                                        Text(
                                            text = if (mins < 60) "${mins}m" else "1h",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (mins == reminderMinutes) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Action buttons ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            titleError = true
                            return@Button
                        }
                        if (kind == SuperKind.TASK) {
                            onSaveTask(title.trim(), description.trim(), priority, deadline)
                        } else {
                            onSaveEvent(
                                title.trim(),
                                description.trim(),
                                eventType,
                                importance,
                                startAt,
                                endAt,
                                hasReminder,
                                reminderMinutes,
                            )
                        }
                    },
                    modifier = Modifier.weight(2f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(
                        text = if (isEdit) "Save" else "Create",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ── Private sub-composables ───────────────────────────────────────────────────

/**
 * A labelled row showing two tappable chips: date and time.
 * An optional clear (×) icon appears when [hasValue] is true and [onClear] is non-null.
 */
@Composable
private fun WhenSection(
    rowLabel: String,
    dateText: String,
    timeText: String,
    hasValue: Boolean,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
    onClear: (() -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = rowLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (hasValue && onClear != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear $rowLabel",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateTimeChip(
                icon = Icons.Outlined.CalendarMonth,
                label = dateText,
                onClick = onPickDate,
                modifier = Modifier.weight(1f),
            )
            DateTimeChip(
                icon = Icons.Outlined.Schedule,
                label = timeText,
                onClick = onPickTime,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DateTimeChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(AppDesignTokens.radius.sm))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun superPriorityColor(priority: TaskPriority): Color =
    when (priority) {
        TaskPriority.URGENT -> Error
        TaskPriority.IMPORTANT -> Warning
        TaskPriority.NEUTRAL -> Info
    }

/**
 * Maps [TaskPriority] to [EventImportance] — the two enums share the same
 * NEUTRAL / IMPORTANT / URGENT semantics, so the mapping is 1-to-1.
 */
private fun TaskPriority.toEventImportance(): EventImportance =
    when (this) {
        TaskPriority.NEUTRAL -> EventImportance.NEUTRAL
        TaskPriority.IMPORTANT -> EventImportance.IMPORTANT
        TaskPriority.URGENT -> EventImportance.URGENT
    }

/**
 * Maps [EventImportance] back to [TaskPriority] for the shared priority chips.
 */
private fun EventImportance.toTaskPriority(): TaskPriority =
    when (this) {
        EventImportance.NEUTRAL -> TaskPriority.NEUTRAL
        EventImportance.IMPORTANT -> TaskPriority.IMPORTANT
        EventImportance.URGENT -> TaskPriority.URGENT
    }

private fun Long.withDate(year: Int, month: Int, day: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = this@withDate
        set(year, month, day)
    }.timeInMillis

private fun Long.withTime(hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = this@withTime
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
