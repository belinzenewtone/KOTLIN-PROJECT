@file:Suppress("LongMethod", "CyclomaticComplexMethod", "LongParameterList", "TooManyFunctions")

package com.personal.lifeOS.features.calendar.presentation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.core.ui.designsystem.AppDesignTokens
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.calendar.domain.model.EventImportance
import com.personal.lifeOS.features.calendar.domain.model.EventKind
import com.personal.lifeOS.features.calendar.domain.model.EventType
import com.personal.lifeOS.features.calendar.domain.model.RepeatRule
import com.personal.lifeOS.features.tasks.domain.model.TaskPriority
import com.personal.lifeOS.ui.theme.Error
import com.personal.lifeOS.ui.theme.Info
import com.personal.lifeOS.ui.theme.Warning
import java.time.LocalDate
import java.util.Calendar
import java.util.TimeZone

// ── Tab types ─────────────────────────────────────────────────────────────────

private enum class AddTab(val label: String) {
    TASK("Task"),
    EVENT("Event"),
    BIRTHDAY("Birthday"),
    ANNIVERSARY("Anniversary"),
    COUNTDOWN("Countdown"),
}

// ── Internal navigation within the add screen ─────────────────────────────────

private enum class AddPage { FORM, REPEAT, REMINDERS }

// ── Named reminder presets (minutes before) ───────────────────────────────────

private data class ReminderPreset(val label: String, val minutes: Int)

private val REMINDER_PRESETS = listOf(
    ReminderPreset("When event starts", 0),
    ReminderPreset("5 minutes before", 5),
    ReminderPreset("10 minutes before", 10),
    ReminderPreset("15 minutes before", 15),
    ReminderPreset("30 minutes before", 30),
    ReminderPreset("1 hour before", 60),
    ReminderPreset("1 day before", 60 * 24),
    ReminderPreset("2 days before", 60 * 24 * 2),
    ReminderPreset("1 week before", 60 * 24 * 7),
)

private val BIRTHDAY_REMINDER_PRESETS = listOf(
    ReminderPreset("On that day", 0),
    ReminderPreset("1 day before", 60 * 24),
    ReminderPreset("2 days before", 60 * 24 * 2),
    ReminderPreset("3 days before", 60 * 24 * 3),
    ReminderPreset("1 week before", 60 * 24 * 7),
)

// ── Root composable ────────────────────────────────────────────────────────────

@Composable
internal fun CalendarAddScreen(
    editingEvent: com.personal.lifeOS.features.calendar.domain.model.CalendarEvent?,
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onSaveTask: (title: String, desc: String, priority: TaskPriority, deadline: Long?) -> Unit,
    onSaveEvent: (
        title: String, desc: String, type: EventType, importance: EventImportance,
        date: Long, endDate: Long?, allDay: Boolean, repeatRule: RepeatRule,
        reminderOffsets: List<Int>, alarmEnabled: Boolean, guests: String, timeZoneId: String,
        kind: EventKind,
    ) -> Unit,
) {
    BackHandler { onDismiss() }

    // ── Tab state ─────────────────────────────────────────────────────────────
    val initialTab = when (editingEvent?.kind) {
        EventKind.BIRTHDAY -> AddTab.BIRTHDAY
        EventKind.ANNIVERSARY -> AddTab.ANNIVERSARY
        EventKind.COUNTDOWN -> AddTab.COUNTDOWN
        else -> if (editingEvent != null) AddTab.EVENT else AddTab.EVENT
    }
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    var currentPage by remember { mutableStateOf(AddPage.FORM) }

    // ── Event form state ──────────────────────────────────────────────────────
    var eventTitle by rememberSaveable(editingEvent?.id) { mutableStateOf(editingEvent?.title.orEmpty()) }
    var eventDesc by rememberSaveable(editingEvent?.id) { mutableStateOf(editingEvent?.description.orEmpty()) }
    var eventType by rememberSaveable(editingEvent?.id) { mutableStateOf(editingEvent?.type ?: EventType.PERSONAL) }
    var eventImportance by rememberSaveable(editingEvent?.id) { mutableStateOf(editingEvent?.importance ?: EventImportance.NEUTRAL) }
    var allDay by rememberSaveable(editingEvent?.id) { mutableStateOf(editingEvent?.allDay ?: false) }
    var startDateTime by remember(editingEvent?.id) {
        mutableLongStateOf(editingEvent?.date ?: defaultEventDateTime(selectedDate))
    }
    var endDateTime by remember(editingEvent?.id) { mutableStateOf<Long?>(editingEvent?.endDate) }
    var repeatRule by rememberSaveable(editingEvent?.id) { mutableStateOf(editingEvent?.repeatRule ?: RepeatRule.NEVER) }
    val reminderOffsets = remember(editingEvent?.id) {
        mutableStateListOf<Int>().also { list -> list.addAll(editingEvent?.reminderOffsets ?: emptyList()) }
    }
    var alarmEnabled by rememberSaveable(editingEvent?.id) { mutableStateOf(editingEvent?.alarmEnabled ?: false) }
    var guests by rememberSaveable(editingEvent?.id) { mutableStateOf(editingEvent?.guests.orEmpty()) }
    var timeZoneId by rememberSaveable(editingEvent?.id) {
        mutableStateOf(editingEvent?.timeZoneId?.ifBlank { TimeZone.getDefault().id } ?: TimeZone.getDefault().id)
    }

    // ── Task form state ───────────────────────────────────────────────────────
    var taskTitle by rememberSaveable { mutableStateOf("") }
    var taskDesc by rememberSaveable { mutableStateOf("") }
    var taskPriority by rememberSaveable { mutableStateOf(TaskPriority.NEUTRAL) }
    var taskDeadline by remember {
        mutableStateOf<Long?>(
            selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() +
                9 * 60 * 60 * 1000L,
        )
    }

    // ── Birthday / Anniversary form state ─────────────────────────────────────
    var bdayName by rememberSaveable { mutableStateOf(editingEvent?.title.orEmpty()) }
    var bdayDate by remember { mutableLongStateOf(editingEvent?.date ?: defaultEventDateTime(selectedDate)) }
    var bdayShowYear by rememberSaveable { mutableStateOf(false) }
    val bdayReminderOffsets = remember { mutableStateListOf<Int>() }
    var bdayAlarm by rememberSaveable { mutableStateOf(false) }

    // ── Countdown form state ──────────────────────────────────────────────────
    var countdownName by rememberSaveable { mutableStateOf("") }
    var countdownDate by remember { mutableLongStateOf(defaultEventDateTime(selectedDate)) }
    var countdownRepeat by rememberSaveable { mutableStateOf(RepeatRule.NEVER) }
    var countdownRemindDaysBefore by rememberSaveable { mutableStateOf(false) }
    var countdownAlarmHour by rememberSaveable { mutableIntStateOf(8) }
    var countdownAlarmMinute by rememberSaveable { mutableIntStateOf(0) }

    // ── Reset page when tab switches ─────────────────────────────────────────
    val onTabSelected: (AddTab) -> Unit = { tab ->
        selectedTab = tab
        currentPage = AddPage.FORM
    }

    // ── Sub-page back handler ─────────────────────────────────────────────────
    if (currentPage != AddPage.FORM) {
        BackHandler { currentPage = AddPage.FORM }
    }

    // ── Root layout ───────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                if (targetState == AddPage.FORM) {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { -it / 4 }) togetherWith
                        (fadeOut(tween(180)) + slideOutHorizontally(tween(240)) { it / 4 })
                } else {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 4 }) togetherWith
                        (fadeOut(tween(180)) + slideOutHorizontally(tween(240)) { -it / 4 })
                }
            },
            label = "addPage",
        ) { page ->
            when (page) {
                AddPage.FORM -> {
                    FormPage(
                        tab = selectedTab,
                        onTabSelected = onTabSelected,
                        onDismiss = onDismiss,
                        isEdit = editingEvent != null,
                        // Event fields
                        eventTitle = eventTitle,
                        onEventTitleChange = { eventTitle = it },
                        eventDesc = eventDesc,
                        onEventDescChange = { eventDesc = it },
                        eventType = eventType,
                        onEventTypeChange = { eventType = it },
                        eventImportance = eventImportance,
                        onEventImportanceChange = { eventImportance = it },
                        allDay = allDay,
                        onAllDayChange = { allDay = it },
                        startDateTime = startDateTime,
                        onStartDateTimeChange = { startDateTime = it },
                        endDateTime = endDateTime,
                        onEndDateTimeChange = { endDateTime = it },
                        repeatRule = repeatRule,
                        onOpenRepeat = { currentPage = AddPage.REPEAT },
                        reminderOffsets = reminderOffsets,
                        onOpenReminders = { currentPage = AddPage.REMINDERS },
                        alarmEnabled = alarmEnabled,
                        onAlarmEnabledChange = { alarmEnabled = it },
                        guests = guests,
                        onGuestsChange = { guests = it },
                        timeZoneId = timeZoneId,
                        onTimeZoneChange = { timeZoneId = it },
                        // Task fields
                        taskTitle = taskTitle,
                        onTaskTitleChange = { taskTitle = it },
                        taskDesc = taskDesc,
                        onTaskDescChange = { taskDesc = it },
                        taskPriority = taskPriority,
                        onTaskPriorityChange = { taskPriority = it },
                        taskDeadline = taskDeadline,
                        onTaskDeadlineChange = { taskDeadline = it },
                        // Birthday fields
                        bdayName = bdayName,
                        onBdayNameChange = { bdayName = it },
                        bdayDate = bdayDate,
                        onBdayDateChange = { bdayDate = it },
                        bdayShowYear = bdayShowYear,
                        onBdayShowYearChange = { bdayShowYear = it },
                        bdayReminderOffsets = bdayReminderOffsets,
                        bdayAlarm = bdayAlarm,
                        onBdayAlarmChange = { bdayAlarm = it },
                        // Countdown fields
                        countdownName = countdownName,
                        onCountdownNameChange = { countdownName = it },
                        countdownDate = countdownDate,
                        onCountdownDateChange = { countdownDate = it },
                        countdownRepeat = countdownRepeat,
                        onOpenCountdownRepeat = { currentPage = AddPage.REPEAT },
                        countdownRemindDaysBefore = countdownRemindDaysBefore,
                        onCountdownRemindDaysBeforeChange = { countdownRemindDaysBefore = it },
                        countdownAlarmHour = countdownAlarmHour,
                        countdownAlarmMinute = countdownAlarmMinute,
                        onCountdownAlarmTimeChange = { h, m -> countdownAlarmHour = h; countdownAlarmMinute = m },
                        // Save callbacks
                        onSaveTask = {
                            if (taskTitle.isNotBlank()) {
                                onSaveTask(taskTitle, taskDesc, taskPriority, taskDeadline)
                            }
                        },
                        onSaveEvent = {
                            if (eventTitle.isNotBlank()) {
                                onSaveEvent(
                                    eventTitle, eventDesc, eventType, eventImportance,
                                    startDateTime, endDateTime, allDay, repeatRule,
                                    reminderOffsets.toList(), alarmEnabled, guests, timeZoneId,
                                    EventKind.EVENT,
                                )
                            }
                        },
                        onSaveBirthday = { kind ->
                            if (bdayName.isNotBlank()) {
                                onSaveEvent(
                                    bdayName, "", EventType.PERSONAL, EventImportance.NEUTRAL,
                                    bdayDate, null, true, RepeatRule.YEARLY,
                                    bdayReminderOffsets.toList(), bdayAlarm, "", "",
                                    kind,
                                )
                            }
                        },
                        onSaveCountdown = {
                            if (countdownName.isNotBlank()) {
                                val alarmOffset = countdownAlarmHour * 60 + countdownAlarmMinute
                                val offsets = if (countdownRemindDaysBefore) listOf(60 * 24 * 3, alarmOffset) else listOf(alarmOffset)
                                onSaveEvent(
                                    countdownName, "", EventType.PERSONAL, EventImportance.NEUTRAL,
                                    countdownDate, null, true, countdownRepeat,
                                    offsets, false, "", "",
                                    EventKind.COUNTDOWN,
                                )
                            }
                        },
                    )
                }

                AddPage.REPEAT -> {
                    RepeatPickerPage(
                        selected = if (selectedTab == AddTab.COUNTDOWN) countdownRepeat else repeatRule,
                        onSelect = { rule ->
                            if (selectedTab == AddTab.COUNTDOWN) countdownRepeat = rule else repeatRule = rule
                            currentPage = AddPage.FORM
                        },
                        onBack = { currentPage = AddPage.FORM },
                    )
                }

                AddPage.REMINDERS -> {
                    val presets = if (selectedTab == AddTab.BIRTHDAY || selectedTab == AddTab.ANNIVERSARY)
                        BIRTHDAY_REMINDER_PRESETS else REMINDER_PRESETS
                    val offsets = if (selectedTab == AddTab.BIRTHDAY || selectedTab == AddTab.ANNIVERSARY)
                        bdayReminderOffsets else reminderOffsets
                    RemindersPickerPage(
                        presets = presets,
                        selectedOffsets = offsets,
                        onToggle = { minutes ->
                            if (offsets.contains(minutes)) offsets.remove(minutes) else offsets.add(minutes)
                        },
                        onBack = { currentPage = AddPage.FORM },
                    )
                }
            }
        }
    }
}

// ── Form page ─────────────────────────────────────────────────────────────────

@Suppress("UnusedParameter")
@Composable
private fun FormPage(
    tab: AddTab,
    onTabSelected: (AddTab) -> Unit,
    onDismiss: () -> Unit,
    isEdit: Boolean,
    // Event
    eventTitle: String, onEventTitleChange: (String) -> Unit,
    eventDesc: String, onEventDescChange: (String) -> Unit,
    eventType: EventType, onEventTypeChange: (EventType) -> Unit,
    eventImportance: EventImportance, onEventImportanceChange: (EventImportance) -> Unit,
    allDay: Boolean, onAllDayChange: (Boolean) -> Unit,
    startDateTime: Long, onStartDateTimeChange: (Long) -> Unit,
    endDateTime: Long?, onEndDateTimeChange: (Long?) -> Unit,
    repeatRule: RepeatRule, onOpenRepeat: () -> Unit,
    reminderOffsets: List<Int>, onOpenReminders: () -> Unit,
    alarmEnabled: Boolean, onAlarmEnabledChange: (Boolean) -> Unit,
    guests: String, onGuestsChange: (String) -> Unit,
    timeZoneId: String, onTimeZoneChange: (String) -> Unit,
    // Task
    taskTitle: String, onTaskTitleChange: (String) -> Unit,
    taskDesc: String, onTaskDescChange: (String) -> Unit,
    taskPriority: TaskPriority, onTaskPriorityChange: (TaskPriority) -> Unit,
    taskDeadline: Long?, onTaskDeadlineChange: (Long?) -> Unit,
    // Birthday
    bdayName: String, onBdayNameChange: (String) -> Unit,
    bdayDate: Long, onBdayDateChange: (Long) -> Unit,
    bdayShowYear: Boolean, onBdayShowYearChange: (Boolean) -> Unit,
    bdayReminderOffsets: List<Int>, bdayAlarm: Boolean, onBdayAlarmChange: (Boolean) -> Unit,
    // Countdown
    countdownName: String, onCountdownNameChange: (String) -> Unit,
    countdownDate: Long, onCountdownDateChange: (Long) -> Unit,
    countdownRepeat: RepeatRule, onOpenCountdownRepeat: () -> Unit,
    countdownRemindDaysBefore: Boolean, onCountdownRemindDaysBeforeChange: (Boolean) -> Unit,
    countdownAlarmHour: Int, countdownAlarmMinute: Int, onCountdownAlarmTimeChange: (Int, Int) -> Unit,
    // Callbacks
    onSaveTask: () -> Unit,
    onSaveEvent: () -> Unit,
    onSaveBirthday: (EventKind) -> Unit,
    onSaveCountdown: () -> Unit,
    onOpenBdayReminders: (() -> Unit) = {},
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = if (isEdit) "Edit ${tab.label}" else "New ${tab.label}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            TextButton(
                onClick = {
                    when (tab) {
                        AddTab.TASK -> onSaveTask()
                        AddTab.EVENT -> onSaveEvent()
                        AddTab.BIRTHDAY -> onSaveBirthday(EventKind.BIRTHDAY)
                        AddTab.ANNIVERSARY -> onSaveBirthday(EventKind.ANNIVERSARY)
                        AddTab.COUNTDOWN -> onSaveCountdown()
                    }
                },
            ) {
                Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }

        // ── Horizontally scrollable type tabs ─────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AddTab.entries.forEach { t ->
                val selected = t == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                        )
                        .clickable { onTabSelected(t) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = t.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(Modifier.height(4.dp))

        // ── Form content ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (tab) {
                AddTab.TASK ->
                    TaskFormContent(
                        title = taskTitle, onTitleChange = onTaskTitleChange,
                        desc = taskDesc, onDescChange = onTaskDescChange,
                        priority = taskPriority, onPriorityChange = onTaskPriorityChange,
                        deadline = taskDeadline, onDeadlineChange = onTaskDeadlineChange,
                    )

                AddTab.EVENT ->
                    EventFormContent(
                        title = eventTitle, onTitleChange = onEventTitleChange,
                        desc = eventDesc, onDescChange = onEventDescChange,
                        type = eventType, onTypeChange = onEventTypeChange,
                        importance = eventImportance, onImportanceChange = onEventImportanceChange,
                        allDay = allDay, onAllDayChange = onAllDayChange,
                        startDateTime = startDateTime, onStartDateTimeChange = onStartDateTimeChange,
                        endDateTime = endDateTime, onEndDateTimeChange = onEndDateTimeChange,
                        repeatRule = repeatRule, onOpenRepeat = onOpenRepeat,
                        reminderOffsets = reminderOffsets, onOpenReminders = onOpenReminders,
                        alarmEnabled = alarmEnabled, onAlarmEnabledChange = onAlarmEnabledChange,
                        guests = guests, onGuestsChange = onGuestsChange,
                        timeZoneId = timeZoneId,
                    )

                AddTab.BIRTHDAY, AddTab.ANNIVERSARY ->
                    BirthdayFormContent(
                        isAnniversary = tab == AddTab.ANNIVERSARY,
                        name = bdayName, onNameChange = onBdayNameChange,
                        date = bdayDate, onDateChange = onBdayDateChange,
                        showYear = bdayShowYear, onShowYearChange = onBdayShowYearChange,
                        reminderOffsets = bdayReminderOffsets,
                        onOpenReminders = onOpenReminders,
                        alarmEnabled = bdayAlarm, onAlarmChange = onBdayAlarmChange,
                    )

                AddTab.COUNTDOWN ->
                    CountdownFormContent(
                        name = countdownName, onNameChange = onCountdownNameChange,
                        date = countdownDate, onDateChange = onCountdownDateChange,
                        repeatRule = countdownRepeat, onOpenRepeat = onOpenCountdownRepeat,
                        remindDaysBefore = countdownRemindDaysBefore,
                        onRemindDaysBeforeChange = onCountdownRemindDaysBeforeChange,
                        alarmHour = countdownAlarmHour, alarmMinute = countdownAlarmMinute,
                        onAlarmTimeChange = onCountdownAlarmTimeChange,
                    )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Task form ─────────────────────────────────────────────────────────────────

@Composable
private fun TaskFormContent(
    title: String, onTitleChange: (String) -> Unit,
    desc: String, onDescChange: (String) -> Unit,
    priority: TaskPriority, onPriorityChange: (TaskPriority) -> Unit,
    deadline: Long?, onDeadlineChange: (Long?) -> Unit,
) {
    val context = LocalContext.current

    FormTextField(value = title, onValueChange = onTitleChange, label = "Task title")
    FormTextField(value = desc, onValueChange = onDescChange, label = "Description (optional)", maxLines = 3)

    FormSectionLabel("Priority")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskPriority.entries.forEach { p ->
            val selected = p == priority
            val color = when (p) {
                TaskPriority.HIGH -> Error
                TaskPriority.MEDIUM -> Warning
                TaskPriority.NEUTRAL -> Info
                TaskPriority.LOW -> MaterialTheme.colorScheme.outlineVariant
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(AppDesignTokens.radius.sm))
                    .background(if (selected) color else color.copy(alpha = 0.15f))
                    .clickable { onPriorityChange(p) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = p.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else color,
                )
            }
        }
    }

    FormSectionLabel("Deadline")
    val deadlineMs = deadline ?: System.currentTimeMillis()
    FormPickerRow(
        icon = Icons.Outlined.CalendarMonth,
        label = "Date",
        value = DateUtils.formatDate(deadlineMs, "MMM dd, yyyy"),
        onClick = {
            val cal = Calendar.getInstance().apply { timeInMillis = deadlineMs }
            DatePickerDialog(
                context,
                { _, y, m, d -> onDeadlineChange(deadlineMs.withDate(y, m, d)) },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
            ).show()
        },
    )
    FormPickerRow(
        icon = Icons.Outlined.AccessTime,
        label = "Time",
        value = DateUtils.formatTime(deadlineMs),
        onClick = {
            val cal = Calendar.getInstance().apply { timeInMillis = deadlineMs }
            TimePickerDialog(
                context,
                { _, h, min -> onDeadlineChange(deadlineMs.withTime(h, min)) },
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false,
            ).show()
        },
    )
}

// ── Event form ────────────────────────────────────────────────────────────────

@Composable
private fun EventFormContent(
    title: String, onTitleChange: (String) -> Unit,
    desc: String, onDescChange: (String) -> Unit,
    type: EventType, onTypeChange: (EventType) -> Unit,
    importance: EventImportance, onImportanceChange: (EventImportance) -> Unit,
    allDay: Boolean, onAllDayChange: (Boolean) -> Unit,
    startDateTime: Long, onStartDateTimeChange: (Long) -> Unit,
    endDateTime: Long?, onEndDateTimeChange: (Long?) -> Unit,
    repeatRule: RepeatRule, onOpenRepeat: () -> Unit,
    reminderOffsets: List<Int>, onOpenReminders: () -> Unit,
    alarmEnabled: Boolean, onAlarmEnabledChange: (Boolean) -> Unit,
    guests: String, onGuestsChange: (String) -> Unit,
    timeZoneId: String,
) {
    val context = LocalContext.current

    FormTextField(value = title, onValueChange = onTitleChange, label = "Event title")

    // All-day toggle
    FormToggleRow(
        label = "All day",
        checked = allDay,
        onCheckedChange = onAllDayChange,
    )

    // When block
    FormSectionLabel("When")
    FormCard {
        FormPickerRow(
            icon = Icons.Outlined.CalendarMonth,
            label = "From",
            value = if (allDay) DateUtils.formatDate(startDateTime, "EEE, MMM dd") else
                DateUtils.formatDate(startDateTime, "EEE, MMM dd") + "  " + DateUtils.formatTime(startDateTime),
            onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = startDateTime }
                DatePickerDialog(
                    context,
                    { _, y, m, d -> onStartDateTimeChange(startDateTime.withDate(y, m, d)) },
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
                ).show()
            },
        )
        if (!allDay) {
            HorizontalDivider(modifier = Modifier.padding(start = 48.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            FormPickerRow(
                icon = Icons.Outlined.AccessTime,
                label = "Start time",
                value = DateUtils.formatTime(startDateTime),
                onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = startDateTime }
                    TimePickerDialog(
                        context,
                        { _, h, min -> onStartDateTimeChange(startDateTime.withTime(h, min)) },
                        cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false,
                    ).show()
                },
            )
        }
        HorizontalDivider(modifier = Modifier.padding(start = 48.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        FormPickerRow(
            icon = Icons.Outlined.CalendarMonth,
            label = "To",
            value = endDateTime?.let {
                if (allDay) DateUtils.formatDate(it, "EEE, MMM dd")
                else DateUtils.formatDate(it, "EEE, MMM dd") + "  " + DateUtils.formatTime(it)
            } ?: "Set end date",
            onClick = {
                val base = endDateTime ?: startDateTime
                val cal = Calendar.getInstance().apply { timeInMillis = base }
                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        val cur = endDateTime ?: startDateTime.withTime(23, 59)
                        onEndDateTimeChange(cur.withDate(y, m, d))
                    },
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
                ).show()
            },
        )
        if (!allDay && endDateTime != null) {
            HorizontalDivider(modifier = Modifier.padding(start = 48.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            FormPickerRow(
                icon = Icons.Outlined.AccessTime,
                label = "End time",
                value = DateUtils.formatTime(endDateTime),
                onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = endDateTime }
                    TimePickerDialog(
                        context,
                        { _, h, min ->
                            onEndDateTimeChange(endDateTime.withTime(h, min))
                        },
                        cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false,
                    ).show()
                },
            )
        }
        if (endDateTime != null) {
            HorizontalDivider(modifier = Modifier.padding(start = 48.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            TextButton(
                onClick = { onEndDateTimeChange(null) },
                modifier = Modifier.padding(start = 36.dp),
            ) {
                Text("Clear end date", color = Error, style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    // Repeat
    FormNavRow(
        icon = Icons.Outlined.Repeat,
        label = "Repeat",
        value = repeatRule.label,
        onClick = onOpenRepeat,
    )

    // Reminders
    FormNavRow(
        icon = Icons.Outlined.Notifications,
        label = "Reminders",
        value = if (reminderOffsets.isEmpty()) "None"
            else "${reminderOffsets.size} reminder${if (reminderOffsets.size > 1) "s" else ""}",
        onClick = onOpenReminders,
    )

    // Alarm
    FormToggleRow(
        icon = Icons.Outlined.NotificationsActive,
        label = "Alarm reminders",
        checked = alarmEnabled,
        onCheckedChange = onAlarmEnabledChange,
    )

    // Guests
    FormTextField(value = guests, onValueChange = onGuestsChange, label = "Add guests (optional)", icon = Icons.Outlined.People)

    // Timezone
    FormCard {
        FormPickerRow(
            icon = Icons.Outlined.Public,
            label = "Time zone",
            value = timeZoneId.ifBlank { TimeZone.getDefault().id },
            onClick = { /* Advanced: open timezone list */ },
        )
    }

    // Category (type)
    FormSectionLabel("Category")
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        EventType.entries.forEach { t ->
            val selected = t == type
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onTypeChange(t) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    text = t.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    // Priority
    FormSectionLabel("Priority")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EventImportance.entries.forEach { imp ->
            val selected = imp == importance
            val color = when (imp) {
                EventImportance.URGENT -> Error
                EventImportance.IMPORTANT -> Warning
                EventImportance.NEUTRAL -> Info
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(AppDesignTokens.radius.sm))
                    .background(if (selected) color else color.copy(alpha = 0.15f))
                    .clickable { onImportanceChange(imp) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = imp.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else color,
                )
            }
        }
    }

    // Description
    FormTextField(value = desc, onValueChange = onDescChange, label = "Description (optional)", maxLines = 4)
}

// ── Birthday / Anniversary form ────────────────────────────────────────────────

@Composable
private fun BirthdayFormContent(
    isAnniversary: Boolean,
    name: String, onNameChange: (String) -> Unit,
    date: Long, onDateChange: (Long) -> Unit,
    showYear: Boolean, onShowYearChange: (Boolean) -> Unit,
    reminderOffsets: List<Int>,
    onOpenReminders: () -> Unit,
    alarmEnabled: Boolean, onAlarmChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val nameLabel = if (isAnniversary) "Anniversary name" else "Person's name"
    val dateLabel = if (isAnniversary) "Date" else "Birthday"

    FormTextField(value = name, onValueChange = onNameChange, label = nameLabel)

    FormSectionLabel(dateLabel)
    FormCard {
        FormPickerRow(
            icon = Icons.Outlined.CalendarMonth,
            label = if (showYear || isAnniversary) "Date" else "Month and day",
            value = DateUtils.formatDate(date, if (showYear || isAnniversary) "MMMM dd, yyyy" else "MMMM dd"),
            onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = date }
                DatePickerDialog(
                    context,
                    { _, y, m, d -> onDateChange(date.withDate(y, m, d)) },
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
                ).show()
            },
        )
    }

    if (!isAnniversary) {
        FormToggleRow(
            label = "Add year",
            checked = showYear,
            onCheckedChange = onShowYearChange,
        )
    }

    FormNavRow(
        icon = Icons.Outlined.Notifications,
        label = "Reminders",
        value = if (reminderOffsets.isEmpty()) "None"
            else "${reminderOffsets.size} reminder${if (reminderOffsets.size > 1) "s" else ""}",
        onClick = onOpenReminders,
    )

    FormToggleRow(
        icon = Icons.Outlined.NotificationsActive,
        label = "Alarm reminders",
        checked = alarmEnabled,
        onCheckedChange = onAlarmChange,
    )
}

// ── Countdown form ────────────────────────────────────────────────────────────

@Composable
private fun CountdownFormContent(
    name: String, onNameChange: (String) -> Unit,
    date: Long, onDateChange: (Long) -> Unit,
    repeatRule: RepeatRule, onOpenRepeat: () -> Unit,
    remindDaysBefore: Boolean, onRemindDaysBeforeChange: (Boolean) -> Unit,
    alarmHour: Int, alarmMinute: Int, onAlarmTimeChange: (Int, Int) -> Unit,
) {
    val context = LocalContext.current

    FormTextField(value = name, onValueChange = onNameChange, label = "Event name")

    FormSectionLabel("When")
    FormPickerRow(
        icon = Icons.Outlined.CalendarMonth,
        label = "Date",
        value = DateUtils.formatDate(date, "EEE, MMM dd, yyyy"),
        onClick = {
            val cal = Calendar.getInstance().apply { timeInMillis = date }
            DatePickerDialog(
                context,
                { _, y, m, d -> onDateChange(date.withDate(y, m, d)) },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
            ).show()
        },
    )

    FormNavRow(
        icon = Icons.Outlined.Repeat,
        label = "Repeat",
        value = repeatRule.label,
        onClick = onOpenRepeat,
    )

    FormSectionLabel("Remind me at")
    FormPickerRow(
        icon = Icons.Outlined.AccessTime,
        label = "Time",
        value = String.format("%02d:%02d", alarmHour, alarmMinute),
        onClick = {
            TimePickerDialog(
                context,
                { _, h, min -> onAlarmTimeChange(h, min) },
                alarmHour, alarmMinute, true,
            ).show()
        },
    )

    FormToggleRow(
        label = "Remind 3 days before",
        checked = remindDaysBefore,
        onCheckedChange = onRemindDaysBeforeChange,
    )
}

// ── Repeat picker sub-page ────────────────────────────────────────────────────

@Composable
private fun RepeatPickerPage(
    selected: RepeatRule,
    onSelect: (RepeatRule) -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SubPageTopBar(title = "Repeat", onBack = onBack)
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            RepeatRule.entries.forEach { rule ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(rule) }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = rule.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (rule == selected) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(start = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                )
            }
        }
    }
}

// ── Reminders picker sub-page ─────────────────────────────────────────────────

@Composable
private fun RemindersPickerPage(
    presets: List<ReminderPreset>,
    selectedOffsets: List<Int>,
    onToggle: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SubPageTopBar(title = "Reminders", onBack = onBack)
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            presets.forEach { preset ->
                val selected = selectedOffsets.contains(preset.minutes)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(preset.minutes) }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = preset.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (selected) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(start = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                )
            }
        }
    }
}

// ── Reusable UI components ─────────────────────────────────────────────────────

@Composable
private fun SubPageTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

@Composable
private fun FormSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    maxLines: Int = 1,
    icon: ImageVector? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        maxLines = maxLines,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDesignTokens.radius.md),
        leadingIcon = if (icon != null) {
            { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDesignTokens.radius.md))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
    ) {
        content()
    }
}

@Composable
private fun FormPickerRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun FormNavRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDesignTokens.radius.md))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun FormToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDesignTokens.radius.md))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary),
        )
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

private fun defaultEventDateTime(selectedDate: LocalDate): Long {
    return Calendar.getInstance().apply {
        set(selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth, 9, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun Long.withDate(year: Int, month: Int, day: Int): Long =
    Calendar.getInstance().apply { timeInMillis = this@withDate; set(year, month, day) }.timeInMillis

private fun Long.withTime(hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = this@withTime
        set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

// ── Slide-up entry animation wrapper (used by CalendarScreen) ─────────────────

@Composable
internal fun CalendarAddScreenOverlay(
    visible: Boolean,
    editingEvent: com.personal.lifeOS.features.calendar.domain.model.CalendarEvent?,
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onSaveTask: (String, String, TaskPriority, Long?) -> Unit,
    onSaveEvent: (String, String, EventType, EventImportance, Long, Long?, Boolean, RepeatRule, List<Int>, Boolean, String, String, EventKind) -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(320)) { it } + fadeIn(tween(280)),
        exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(220)),
    ) {
        CalendarAddScreen(
            editingEvent = editingEvent,
            selectedDate = selectedDate,
            onDismiss = onDismiss,
            onSaveTask = onSaveTask,
            onSaveEvent = onSaveEvent,
        )
    }
}
