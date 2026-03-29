package com.personal.lifeOS.features.tasks.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.EmptyState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.core.ui.designsystem.SearchField
import com.personal.lifeOS.core.ui.designsystem.SuperAddBottomSheet
import com.personal.lifeOS.core.ui.designsystem.SuperKind
import com.personal.lifeOS.core.ui.designsystem.TaskRow
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.tasks.domain.model.Task
import com.personal.lifeOS.features.tasks.domain.model.TaskPriority
import com.personal.lifeOS.ui.components.StyledSnackbarHost
import com.personal.lifeOS.ui.theme.AppSpacing

@Composable
fun TasksScreen(viewModel: TasksViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var query by rememberSaveable { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Task?>(null) }

    val pendingTasks = remember(state.pendingTasks, query) { state.pendingTasks.filterForQuery(query) }
    val completedTasks = remember(state.completedTasks, query) { state.completedTasks.filterForQuery(query) }

    LaunchedEffect(state.error, state.successMessage) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { StyledSnackbarHost(snackbarHostState) },
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
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        },
    ) { padding ->
        TasksBody(
            state = state,
            pendingTasks = pendingTasks,
            completedTasks = completedTasks,
            query = query,
            padding = padding,
            onQueryChange = { query = it },
            onEditTask = { task -> viewModel.showEditDialog(task) },
            onCompleteTask = { task -> viewModel.completeTask(task) },
            onDeleteTask = { task -> deleteTarget = task },
            onUndoTask = { task -> viewModel.undoComplete(task) },
        )
    }

    // ── Delete confirmation dialog ─────────────────────────────────────────
    deleteTarget?.let { task ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete task?") },
            text = { Text("Remove \"${task.title}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(task)
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

    if (state.showDialog) {
        SuperAddBottomSheet(
            defaultKind = SuperKind.TASK,
            isEdit = state.editingTask != null,
            editTitle = state.title,
            editDescription = state.description,
            editPriority = state.priority,
            editDeadline = state.deadline,
            onDismiss = { viewModel.hideDialog() },
            onSaveTask = { title, desc, priority, deadline ->
                viewModel.saveTaskWith(title, desc, priority, deadline)
            },
            onSaveEvent = { _, _, _, _, _, _, _, _ ->
                // Tasks screen never saves events — no-op
            },
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun TasksBody(
    state: TasksUiState,
    pendingTasks: List<Task>,
    completedTasks: List<Task>,
    query: String,
    padding: PaddingValues,
    onQueryChange: (String) -> Unit,
    onEditTask: (Task) -> Unit,
    onCompleteTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onUndoTask: (Task) -> Unit,
) {
    PageScaffold(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding),
        title = "Tasks",
        subtitle = "${state.pendingTasks.size} pending • ${state.completedTasks.size} completed",
        contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFab),
    ) {
        SearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "Search tasks",
        )

        if (pendingTasks.isEmpty()) {
            EmptyState(
                title = "No pending tasks",
                description = "Create a task to start your daily focus list.",
            )
        } else {
            // Group by priority — URGENT first, then IMPORTANT, then NEUTRAL
            val priorityOrder = listOf(TaskPriority.URGENT, TaskPriority.IMPORTANT, TaskPriority.NEUTRAL)
            val grouped = pendingTasks.groupBy { it.priority }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                priorityOrder.forEach { priority ->
                    val group = grouped[priority] ?: return@forEach
                    PrioritySection(
                        priority = priority,
                        tasks = group,
                        onEditTask = onEditTask,
                        onCompleteTask = onCompleteTask,
                        onDeleteTask = onDeleteTask,
                    )
                }
            }
        }

        if (completedTasks.isNotEmpty()) {
            Text(
                text = "Completed",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                completedTasks.take(20).forEach { task ->
                    TaskRow(
                        title = task.title,
                        subtitle = task.subtitle(),
                        isCompleted = true,
                        onToggleComplete = { onUndoTask(task) },
                        onClick = { onUndoTask(task) },
                    )
                }
            }
        }
    }
}

/**
 * Renders a labelled priority group: a small colour-coded header followed by the task rows.
 * Only renders if [tasks] is non-empty.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("CyclomaticComplexMethod", "LongMethod")
@Composable
private fun PrioritySection(
    priority: TaskPriority,
    tasks: List<Task>,
    onEditTask: (Task) -> Unit,
    onCompleteTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
) {
    if (tasks.isEmpty()) return

    val (label, labelColor) = when (priority) {
        TaskPriority.URGENT -> "Urgent" to MaterialTheme.colorScheme.error
        TaskPriority.IMPORTANT -> "Important" to Color(0xFFF59E0B) // amber
        TaskPriority.NEUTRAL -> "Neutral" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = labelColor,
        )
        tasks.forEach { task ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    when (value) {
                        SwipeToDismissBoxValue.StartToEnd -> {
                            if (task.status != com.personal.lifeOS.features.tasks.domain.model.TaskStatus.COMPLETED) {
                                onCompleteTask(task)
                            }
                            false
                        }
                        SwipeToDismissBoxValue.EndToStart -> {
                            onDeleteTask(task)
                            false
                        }
                        else -> false
                    }
                },
                positionalThreshold = { it * 0.40f },
            )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val direction = dismissState.dismissDirection
                    val bgColor by animateColorAsState(
                        when (direction) {
                            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                            else -> Color.Transparent
                        },
                        label = "swipe_bg",
                    )
                    val icon = when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> Icons.Filled.CheckCircle
                        SwipeToDismissBoxValue.EndToStart -> Icons.Filled.Delete
                        else -> null
                    }
                    val alignment = when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                        else -> Alignment.CenterEnd
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(bgColor)
                            .padding(horizontal = 20.dp),
                        contentAlignment = alignment,
                    ) {
                        icon?.let {
                            Icon(
                                it,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
            ) {
                TaskRow(
                    title = task.title,
                    subtitle = task.subtitle(),
                    isCompleted = false,
                    priority = task.priority.name,
                    onToggleComplete = { onCompleteTask(task) },
                    onClick = { onEditTask(task) },
                )
            }
        }
    }
}

private fun List<Task>.filterForQuery(query: String): List<Task> {
    if (query.isBlank()) return this
    val trimmed = query.trim()
    return filter { task ->
        task.title.contains(trimmed, ignoreCase = true) ||
            task.description.contains(trimmed, ignoreCase = true)
    }
}

private fun Task.subtitle(): String {
    return when {
        description.isNotBlank() -> description
        deadline != null -> "Due ${DateUtils.formatDate(deadline, "MMM dd, h:mm a")}"
        else -> "No deadline"
    }
}
