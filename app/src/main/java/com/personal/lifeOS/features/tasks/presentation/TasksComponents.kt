package com.personal.lifeOS.features.tasks.presentation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.unit.sp
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.tasks.domain.model.TaskPriority
import com.personal.lifeOS.ui.theme.BackgroundDark
import com.personal.lifeOS.ui.theme.Error
import com.personal.lifeOS.ui.theme.GlassBorder
import com.personal.lifeOS.ui.theme.GlassWhite
import com.personal.lifeOS.ui.theme.Primary
import com.personal.lifeOS.ui.theme.SurfaceDark
import com.personal.lifeOS.ui.theme.TextPrimary
import com.personal.lifeOS.ui.theme.TextSecondary
import com.personal.lifeOS.ui.theme.TextTertiary
import java.util.Calendar

private fun priorityColor(priority: TaskPriority): Color =
    when (priority) {
        TaskPriority.URGENT -> Color(0xFFEF5350)
        TaskPriority.IMPORTANT -> Color(0xFFFFB74D)
        TaskPriority.NEUTRAL -> Color(0xFF42A5F5)
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
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(24.dp),
        title = { Text(if (isEdit) "Edit Task" else "New Task", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { viewModel.setTitle(it) },
                    label = { Text("Title") },
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
                    value = state.description,
                    onValueChange = { viewModel.setDescription(it) },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
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

                Text("Priority", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { priority ->
                        val isSelected = priority == state.priority
                        Row(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
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
                                fontSize = 13.sp,
                                color = if (isSelected) BackgroundDark else priorityColor(priority),
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
                        icon = Icons.Filled.CalendarMonth,
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
                        icon = Icons.Filled.Timer,
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
                        containerColor = Primary,
                        contentColor = BackgroundDark,
                    ),
            ) {
                Text(if (isEdit) "Save" else "Create", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.hideDialog() }) {
                Text("Cancel", color = TextSecondary)
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
                .clip(RoundedCornerShape(16.dp))
                .background(GlassWhite)
                .clickable(onClick = onClick)
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
            )
        }
    }
}
