package com.personal.lifeOS.features.tasks.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.ui.components.StyledSnackbarHost
import com.personal.lifeOS.ui.theme.AppSpacing
import com.personal.lifeOS.ui.theme.BackgroundDark
import com.personal.lifeOS.ui.theme.Primary
import com.personal.lifeOS.ui.theme.TextPrimary

@Composable
fun TasksScreen(viewModel: TasksViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .padding(horizontal = AppSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFab),
        ) {
            item {
                Spacer(Modifier.height(AppSpacing.ScreenTop))
                TasksHeader(
                    pendingCount = state.pendingTasks.size,
                    completedCount = state.completedTasks.size,
                )
                Spacer(Modifier.height(AppSpacing.Section))
            }

            if (state.pendingTasks.isEmpty() && !state.isLoading) {
                item { PendingTasksEmptyState() }
            }

            items(state.pendingTasks, key = { it.id }) { task ->
                SwipeTaskItem(
                    task = task,
                    onComplete = { viewModel.completeTask(task) },
                    onDelete = { viewModel.deleteTask(task) },
                    onEdit = { viewModel.showEditDialog(task) },
                )
            }

            if (state.completedTasks.isNotEmpty()) {
                item { CompletedSectionHeader() }
                items(state.completedTasks.take(10), key = { it.id }) { task ->
                    CompletedTaskItem(
                        task = task,
                        onUndo = { viewModel.undoComplete(task) },
                    )
                }
            }
        }
    }

    if (state.showDialog) {
        TaskDialog(state = state, viewModel = viewModel)
    }
}
