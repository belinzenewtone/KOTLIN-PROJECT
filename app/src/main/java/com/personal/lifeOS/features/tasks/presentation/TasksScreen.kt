package com.personal.lifeOS.features.tasks.presentation

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.tasks.domain.model.Task
import com.personal.lifeOS.features.tasks.domain.model.TaskPriority
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.*

@Composable
fun TasksScreen(
    viewModel: TasksViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = Primary,
                contentColor = TextPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, "Add task")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Text("Tasks", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "${state.pendingTasks.size} pending · ${state.completedTasks.size} completed",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
                Spacer(Modifier.height(8.dp))
            }

            // Empty state
            if (state.pendingTasks.isEmpty() && !state.isLoading) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No pending tasks", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Tap + to create your first task",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Pending tasks
            items(state.pendingTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onComplete = { viewModel.completeTask(task) },
                    onDelete = { viewModel.deleteTask(task) }
                )
            }

            // Completed section
            if (state.completedTasks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.toggleShowCompleted() }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Completed (${state.completedTasks.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            if (state.showCompleted) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            "Toggle",
                            tint = TextSecondary
                        )
                    }
                }

                if (state.showCompleted) {
                    items(state.completedTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onComplete = {},
                            onDelete = { viewModel.deleteTask(task) },
                            isCompleted = true
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }

    if (state.showAddDialog) {
        AddTaskDialog(
            onDismiss = { viewModel.hideAddDialog() },
            onAdd = { title, desc, priority -> viewModel.addTask(title, desc, priority) }
        )
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    isCompleted: Boolean = false
) {
    val priorityColor = when (task.priority) {
        TaskPriority.CRITICAL -> Error
        TaskPriority.HIGH -> Warning
        TaskPriority.MEDIUM -> Primary
        TaskPriority.LOW -> TextTertiary
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            IconButton(
                onClick = onComplete,
                modifier = Modifier.size(32.dp),
                enabled = !isCompleted
            ) {
                Icon(
                    if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    "Complete",
                    tint = if (isCompleted) Success else TextTertiary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isCompleted) TextTertiary else TextPrimary
                )
                if (task.description.isNotBlank()) {
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 2
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Priority badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(priorityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            task.priority.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor
                        )
                    }
                    if (task.deadline != null) {
                        Text(
                            "Due ${DateUtils.formatDate(task.deadline, "MMM dd")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Delete, "Delete", tint = Error.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, TaskPriority) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("New Task", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = GlassBorder
                    )
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = GlassBorder
                    )
                )
                Text("Priority", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskPriority.entries.forEach { p ->
                        val color = when (p) {
                            TaskPriority.CRITICAL -> Error
                            TaskPriority.HIGH -> Warning
                            TaskPriority.MEDIUM -> Primary
                            TaskPriority.LOW -> TextTertiary
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (p == priority) color else GlassWhite)
                                .clickable { priority = p }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                p.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (p == priority) BackgroundDark else TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onAdd(title, description, priority) }
            ) { Text("Create", color = Primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
