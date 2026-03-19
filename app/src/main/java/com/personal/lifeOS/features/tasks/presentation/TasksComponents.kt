package com.personal.lifeOS.features.tasks.presentation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.tasks.domain.model.Task
import com.personal.lifeOS.features.tasks.domain.model.TaskPriority
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.BackgroundDark
import com.personal.lifeOS.ui.theme.Error
import com.personal.lifeOS.ui.theme.GlassBorder
import com.personal.lifeOS.ui.theme.GlassWhite
import com.personal.lifeOS.ui.theme.Primary
import com.personal.lifeOS.ui.theme.Success
import com.personal.lifeOS.ui.theme.SurfaceDark
import com.personal.lifeOS.ui.theme.TextPrimary
import com.personal.lifeOS.ui.theme.TextSecondary
import com.personal.lifeOS.ui.theme.TextTertiary
import com.personal.lifeOS.ui.theme.Warning
import kotlinx.coroutines.delay
import java.util.Calendar

private fun priorityColor(priority: TaskPriority): Color =
    when (priority) {
        TaskPriority.URGENT -> Color(0xFFEF5350)
        TaskPriority.IMPORTANT -> Color(0xFFFFB74D)
        TaskPriority.NEUTRAL -> Color(0xFF42A5F5)
    }

@Composable
internal fun TasksHeader(
    pendingCount: Int,
    completedCount: Int,
) {
    Text("Tasks", style = MaterialTheme.typography.headlineLarge)
    Text(
        "$pendingCount pending · $completedCount completed",
        style = MaterialTheme.typography.bodyLarge,
        color = TextSecondary,
    )
}

@Composable
internal fun PendingTasksEmptyState() {
    GlassCard(Modifier.fillMaxWidth()) {
        Column {
            Text("No pending tasks", style = MaterialTheme.typography.titleMedium)
            Text(
                "Tap + to create your first task",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}

@Composable
internal fun CompletedSectionHeader() {
    Spacer(Modifier.height(12.dp))
    Text(
        "Completed",
        style = MaterialTheme.typography.titleMedium,
        color = TextTertiary,
    )
}

@Composable
internal fun CompletedTaskItem(
    task: Task,
    onUndo: () -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth().clickable(onClick = onUndo)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, null, tint = Success, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = task.title,
                textDecoration = TextDecoration.LineThrough,
                color = TextTertiary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun SwipeTaskItem(
    task: Task,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    val dismissState =
        rememberSwipeToDismissBoxState(
            positionalThreshold = { totalDistance -> totalDistance * 0.4f },
            confirmValueChange = { target ->
                when (target) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        onComplete()
                        true
                    }

                    SwipeToDismissBoxValue.EndToStart -> {
                        onDelete()
                        true
                    }

                    else -> false
                }
            },
        )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val backgroundColor by animateColorAsState(
                targetValue =
                    when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> Success.copy(alpha = 0.3f)
                        SwipeToDismissBoxValue.EndToStart -> Error.copy(alpha = 0.3f)
                        else -> Color.Transparent
                    },
                label = "task-swipe-color",
            )
            val icon =
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Icons.Filled.CheckCircle
                } else {
                    Icons.Filled.Delete
                }
            val iconAlignment =
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(backgroundColor)
                        .padding(horizontal = 20.dp),
                contentAlignment = iconAlignment,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
    ) {
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .width(4.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(priorityColor(task.priority)),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(priorityColor(task.priority).copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = task.priority.label,
                                fontSize = 10.sp,
                                color = priorityColor(task.priority),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        task.deadline?.let { deadline ->
                            CountdownText(deadline = deadline)
                        }
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit",
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownText(deadline: Long) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000)
        }
    }

    val remaining = deadline - now
    val color =
        when {
            remaining < 0 -> Error
            remaining < 3_600_000 -> Error
            remaining < 86_400_000 -> Warning
            else -> TextTertiary
        }

    val text =
        when {
            remaining < 0 -> "Overdue"
            remaining < 3_600_000 -> "${remaining / 60_000}m left"
            remaining < 86_400_000 -> "${remaining / 3_600_000}h left"
            else -> "${remaining / 86_400_000}d left"
        }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Timer,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = text,
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
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
                        Box(
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
                                    )
                                    .clickable { viewModel.setPriority(priority) }
                                    .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
