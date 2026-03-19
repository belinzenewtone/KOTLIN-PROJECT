package com.personal.lifeOS.features.tasks.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.EmptyState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.core.ui.designsystem.SearchField
import com.personal.lifeOS.core.ui.designsystem.TaskRow
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.tasks.domain.model.Task
import com.personal.lifeOS.ui.components.StyledSnackbarHost
import com.personal.lifeOS.ui.theme.AppSpacing
import com.personal.lifeOS.ui.theme.BackgroundDark
import com.personal.lifeOS.ui.theme.Primary
import com.personal.lifeOS.ui.theme.TextPrimary

@Composable
fun TasksScreen(viewModel: TasksViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var query by rememberSaveable { mutableStateOf("") }

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
        containerColor = BackgroundDark,
        snackbarHost = { StyledSnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                modifier =
                    Modifier
                        .navigationBarsPadding()
                        .padding(bottom = AppSpacing.FabBottomOffset),
                onClick = { viewModel.showAddDialog() },
                containerColor = Primary,
                contentColor = TextPrimary,
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
            onDeleteTask = { task -> viewModel.deleteTask(task) },
            onUndoTask = { task -> viewModel.undoComplete(task) },
        )
    }

    if (state.showDialog) {
        TaskDialog(state = state, viewModel = viewModel)
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
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pendingTasks.forEach { task ->
                    TaskRow(
                        title = task.title,
                        subtitle = task.subtitle(),
                        isCompleted = false,
                        onToggleComplete = { onCompleteTask(task) },
                        onClick = { onEditTask(task) },
                    )
                    androidx.compose.material3.TextButton(onClick = { onDeleteTask(task) }) {
                        androidx.compose.material3.Text("Delete")
                    }
                }
            }
        }

        if (completedTasks.isNotEmpty()) {
            androidx.compose.material3.Text(
                text = "Completed",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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
