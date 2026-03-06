package com.personal.lifeOS.features.tasks.presentation

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.tasks.domain.model.Task
import com.personal.lifeOS.features.tasks.domain.model.TaskPriority
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Calendar

// Color coding: Urgent=Red, Important=Amber, Neutral=Blue
private fun priorityColor(p: TaskPriority): Color = when (p) {
    TaskPriority.URGENT -> Color(0xFFEF5350)
    TaskPriority.IMPORTANT -> Color(0xFFFFB74D)
    TaskPriority.NEUTRAL -> Color(0xFF42A5F5)
}

@Composable
fun TasksScreen(viewModel: TasksViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error, state.successMessage) {
        state.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
        state.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        containerColor = BackgroundDark,
        snackbarHost = { com.personal.lifeOS.ui.components.StyledSnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.padding(bottom = 80.dp),
                onClick = { viewModel.showAddDialog() },
                containerColor = Primary, contentColor = TextPrimary, shape = CircleShape
            ) { Icon(Icons.Filled.Add, "Add") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Text("Tasks", style = MaterialTheme.typography.headlineLarge)
                Text("${state.pendingTasks.size} pending · ${state.completedTasks.size} completed",
                    style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                Spacer(Modifier.height(12.dp))
            }

            if (state.pendingTasks.isEmpty() && !state.isLoading) {
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column {
                            Text("No pending tasks", style = MaterialTheme.typography.titleMedium)
                            Text("Tap + to create your first task", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }
            }

            items(state.pendingTasks, key = { it.id }) { task ->
                SwipeTaskItem(task, { viewModel.completeTask(task) }, { viewModel.deleteTask(task) }, { viewModel.showEditDialog(task) })
            }

            if (state.completedTasks.isNotEmpty()) {
                item { Spacer(Modifier.height(12.dp)); Text("Completed", style = MaterialTheme.typography.titleMedium, color = TextTertiary) }
                items(state.completedTasks.take(10), key = { it.id }) { task ->
                    GlassCard(Modifier.fillMaxWidth().clickable { viewModel.undoComplete(task) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, null, tint = Success, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(task.title, textDecoration = TextDecoration.LineThrough, color = TextTertiary, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    if (state.showDialog) TaskDialog(state, viewModel)
}

@Composable
private fun SwipeTaskItem(task: Task, onComplete: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.4f },
        confirmValueChange = {
        when (it) { SwipeToDismissBoxValue.StartToEnd -> { onComplete(); true }; SwipeToDismissBoxValue.EndToStart -> { onDelete(); true }; else -> false }
    })

    SwipeToDismissBox(state = dismissState, backgroundContent = {
        val dir = dismissState.dismissDirection
        val color by animateColorAsState(when (dir) {
            SwipeToDismissBoxValue.StartToEnd -> Success.copy(0.3f); SwipeToDismissBoxValue.EndToStart -> Error.copy(0.3f); else -> Color.Transparent }, label = "")
        val icon = if (dir == SwipeToDismissBoxValue.StartToEnd) Icons.Filled.CheckCircle else Icons.Filled.Delete
        val align = if (dir == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(color).padding(horizontal = 20.dp), contentAlignment = align) {
            Icon(icon, null, tint = TextPrimary, modifier = Modifier.size(28.dp))
        }
    }) {
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Priority color bar
                Box(Modifier.width(4.dp).height(48.dp).clip(RoundedCornerShape(2.dp)).background(priorityColor(task.priority)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (task.description.isNotBlank()) {
                        Text(task.description, style = MaterialTheme.typography.bodySmall, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Priority badge
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(priorityColor(task.priority).copy(0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(task.priority.label, fontSize = 10.sp, color = priorityColor(task.priority), fontWeight = FontWeight.SemiBold)
                        }
                        // Countdown timer
                        task.deadline?.let { deadline ->
                            CountdownText(deadline)
                        }
                    }
                }
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit", tint = TextTertiary, modifier = Modifier.size(18.dp)) }
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
            delay(60_000) // update every minute
        }
    }

    val remaining = deadline - now
    val color = when {
        remaining < 0 -> Error
        remaining < 3600000 -> Error // < 1 hour
        remaining < 86400000 -> Warning // < 1 day
        else -> TextTertiary
    }

    val text = when {
        remaining < 0 -> "Overdue"
        remaining < 3600000 -> "${remaining / 60000}m left"
        remaining < 86400000 -> "${remaining / 3600000}h left"
        else -> "${remaining / 86400000}d left"
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Timer, null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(3.dp))
        Text(text, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TaskDialog(state: TasksUiState, viewModel: TasksViewModel) {
    val context = LocalContext.current
    val isEdit = state.editingTask != null

    AlertDialog(
        onDismissRequest = { viewModel.hideDialog() },
        containerColor = SurfaceDark, shape = RoundedCornerShape(24.dp),
        title = { Text(if (isEdit) "Edit Task" else "New Task", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = state.title, onValueChange = { viewModel.setTitle(it) },
                    label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary, unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = GlassWhite, unfocusedContainerColor = GlassWhite, cursorColor = Primary))

                OutlinedTextField(value = state.description, onValueChange = { viewModel.setDescription(it) },
                    label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth(), maxLines = 3,
                    shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary, unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = GlassWhite, unfocusedContainerColor = GlassWhite, cursorColor = Primary))

                // Priority: 3 choices with color coding
                Text("Priority", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { p ->
                        val sel = p == state.priority
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (sel) priorityColor(p) else priorityColor(p).copy(0.15f))
                            .clickable { viewModel.setPriority(p) }
                            .padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(p.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                color = if (sel) BackgroundDark else priorityColor(p))
                        }
                    }
                }

                // Date picker
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GlassWhite)
                    .clickable {
                        val cal = Calendar.getInstance(); state.deadline?.let { cal.timeInMillis = it }
                        DatePickerDialog(context, { _, y, m, d ->
                            viewModel.setDeadline(Calendar.getInstance().apply { set(y, m, d, 23, 59, 59) }.timeInMillis)
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(if (state.deadline != null) "Deadline: ${DateUtils.formatDate(state.deadline, "MMM dd, yyyy")}" else "Set deadline (optional)",
                        color = if (state.deadline != null) TextPrimary else TextTertiary)
                }
                if (state.deadline != null) {
                    TextButton(onClick = { viewModel.setDeadline(null) }) { Text("Clear deadline", color = Error) }
                }
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.saveTask() }, colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = BackgroundDark)) {
                Text(if (isEdit) "Save" else "Create", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = { viewModel.hideDialog() }) { Text("Cancel", color = TextSecondary) } }
    )
}
