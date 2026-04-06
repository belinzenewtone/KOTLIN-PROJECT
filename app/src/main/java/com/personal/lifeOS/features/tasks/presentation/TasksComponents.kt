package com.personal.lifeOS.features.tasks.presentation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.personal.lifeOS.features.tasks.domain.model.TaskPriority
import com.personal.lifeOS.ui.theme.Error
import com.personal.lifeOS.ui.theme.Info
import com.personal.lifeOS.ui.theme.Warning
import java.util.Calendar

private fun priorityColor(priority: TaskPriority): Color =
    when (priority) {
        TaskPriority.URGENT -> Error
        TaskPriority.IMPORTANT -> Warning
        TaskPriority.NEUTRAL -> Info
    }

@Composable
@Suppress("CyclomaticComplexMethod", "LongMethod")
internal fun TaskDialog(
    state: TasksUiState,
    viewModel: TasksViewModel,
) {
    val context = LocalContext.current
    val isEdit = state.editingTask != null

    AlertDialog(
        onDismissRequest = { viewModel.hideDialog() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(AppDesignTokens.radius.lg),
        title = { Text(if (isEdit) "Edit Task" else "New Task", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { viewModel.setTitle(it) },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppDesignTokens.radius.md),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                )

                OutlinedTextField(
                    value = state.description,
                    onValueChange = { viewModel.setDescription(it) },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(AppDesignTokens.radius.md),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                )

                Text("Priority", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { priority ->
                        val isSelected = priority == state.priority
                        Row(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(AppDesignTokens.radius.sm))
                                    .background(
                                        if (isSelected) {
                                            priorityColor(priority)
                                        } else {
                                            priorityColor(priority).copy(alpha = 0.15f)
                                        },
                                    ).clickable { viewModel.setPriority(priority) }
                                    .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = priority.label,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else priorityColor(priority),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DeadlinePickerRow(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.CalendarMonth,
                        label = "Due Date",
                        value =
                            if (state.deadline != null) {
                                DateUtils.formatDate(state.deadline, "MMM dd, yyyy")
                            } else {
                                "Set date"
                            },
                        onClick = {
                            val calendar = Calendar.getInstance()
                            state.deadline?.let { calendar.timeInMillis = it }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val updated =
                                        Calendar.getInstance().apply {
                                            if (state.deadline != null) {
                                                timeInMillis = state.deadline
                                            } else {
                                                set(Calendar.HOUR_OF_DAY, 23)
                                                set(Calendar.MINUTE, 59)
                                                set(Calendar.SECOND, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            set(year, month, day)
                                        }.timeInMillis
                                    viewModel.setDeadline(updated)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH),
                            ).show()
                        },
                    )

                    DeadlinePickerRow(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Timer,
                        label = "Due Time",
                        value =
                            if (state.deadline != null) {
                                DateUtils.formatTime(state.deadline)
                            } else {
                                "Set time"
                            },
                        onClick = {
                            val calendar = Calendar.getInstance()
                            state.deadline?.let { calendar.timeInMillis = it }
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    val updated =
                                        Calendar.getInstance().apply {
                                            if (state.deadline != null) {
                                                timeInMillis = state.deadline
                                            } else {
                                                set(Calendar.SECOND, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            set(Calendar.HOUR_OF_DAY, hour)
                                            set(Calendar.MINUTE, minute)
                                        }.timeInMillis
                                    viewModel.setDeadline(updated)
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                false,
                            ).show()
                        },
                    )
                }

                if (state.deadline != null) {
                    TextButton(onClick = { viewModel.setDeadline(null) }) {
                        Text("Clear deadline", color = Error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.saveTask() },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.background,
                    ),
            ) {
                Text(if (isEdit) "Save" else "Create", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.hideDialog() }) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
private fun DeadlinePickerRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(AppDesignTokens.radius.md))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick)
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
