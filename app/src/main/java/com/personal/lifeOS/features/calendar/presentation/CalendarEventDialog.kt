@file:Suppress("LongMethod", "CyclomaticComplexMethod")

package com.personal.lifeOS.features.calendar.presentation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.lifeOS.core.ui.designsystem.AppDesignTokens
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.calendar.domain.model.CalendarEvent
import com.personal.lifeOS.features.calendar.domain.model.EventImportance
import com.personal.lifeOS.features.calendar.domain.model.EventType
import com.personal.lifeOS.ui.theme.Error
import com.personal.lifeOS.ui.theme.Info
import com.personal.lifeOS.ui.theme.Warning
import java.time.LocalDate
import java.util.Calendar

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
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(AppDesignTokens.radius.lg),
        title = { Text(if (isEdit) "Edit Event" else "New Event", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppDesignTokens.radius.md),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppDesignTokens.radius.md),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                )

                Text("Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    EventType.entries.forEach { type ->
                        Row(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(AppDesignTokens.radius.sm))
                                    .background(if (type == selectedType) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedType = type }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = type.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (type == selectedType) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Text("Priority", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventImportance.entries.forEach { importance ->
                        val selected = importance == selectedImportance
                        Row(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(AppDesignTokens.radius.sm))
                                    .background(
                                        if (selected) {
                                            importanceColor(importance)
                                        } else {
                                            importanceColor(importance).copy(alpha = 0.15f)
                                        },
                                    ).clickable { selectedImportance = importance }
                                    .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = importance.label,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else importanceColor(importance),
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
                                    eventDateTime.withDate(
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
                Text(if (isEdit) "Save" else "Add", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
private fun DateTimePickerRow(
    title: String,
    icon: ImageVector,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppDesignTokens.radius.md))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun importanceColor(importance: EventImportance) =
    when (importance) {
        EventImportance.URGENT -> Error
        EventImportance.IMPORTANT -> Warning
        EventImportance.NEUTRAL -> Info
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
