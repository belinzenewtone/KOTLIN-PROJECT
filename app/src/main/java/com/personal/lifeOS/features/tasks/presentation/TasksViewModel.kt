package com.personal.lifeOS.features.tasks.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.features.tasks.domain.model.Task
import com.personal.lifeOS.features.tasks.domain.model.TaskPriority
import com.personal.lifeOS.features.tasks.domain.model.TaskStatus
import com.personal.lifeOS.features.tasks.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasksUiState(
    val pendingTasks: List<Task> = emptyList(),
    val completedTasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val showDialog: Boolean = false,
    val editingTask: Task? = null,
    val title: String = "",
    val description: String = "",
    val priority: TaskPriority = TaskPriority.NEUTRAL,
    val deadline: Long? = null,
    val error: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class TasksViewModel
    @Inject
    constructor(
        private val repository: TaskRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(TasksUiState())
        val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

        init {
            loadTasks()
        }

        fun onEvent(event: TasksUiEvent) {
            when (event) {
                TasksUiEvent.AddTask -> showAddDialog()
                is TasksUiEvent.EditTask -> {
                    _uiState.value.pendingTasks.firstOrNull { it.id == event.taskId }?.let(::showEditDialog)
                }
                is TasksUiEvent.CompleteTask -> {
                    _uiState.value.pendingTasks.firstOrNull { it.id == event.taskId }?.let(::completeTask)
                }
                is TasksUiEvent.DeleteTask -> {
                    (_uiState.value.pendingTasks + _uiState.value.completedTasks)
                        .firstOrNull { it.id == event.taskId }
                        ?.let(::deleteTask)
                }
                TasksUiEvent.DismissDialog -> hideDialog()
            }
        }

        fun showAddDialog() {
            _uiState.update {
                it.copy(
                    showDialog = true,
                    editingTask = null,
                    title = "",
                    description = "",
                    priority = TaskPriority.NEUTRAL,
                    deadline = null,
                )
            }
        }

        fun showEditDialog(task: Task) {
            _uiState.update {
                it.copy(
                    showDialog = true,
                    editingTask = task,
                    title = task.title,
                    description = task.description,
                    priority = task.priority,
                    deadline = task.deadline,
                )
            }
        }

        /**
         * Called by search deep-links. Finds the task by [taskId] from the current loaded lists
         * and opens its edit dialog. No-ops if the task isn't loaded yet (unlikely since tasks
         * load on init, but the LaunchedEffect in TasksScreen retries after loading completes).
         */
        fun showEditDialogById(taskId: Long) {
            val task =
                (_uiState.value.pendingTasks + _uiState.value.completedTasks)
                    .firstOrNull { it.id == taskId }
            task?.let { showEditDialog(it) }
        }

        fun hideDialog() {
            _uiState.update { it.copy(showDialog = false, editingTask = null) }
        }

        fun setTitle(v: String) {
            _uiState.update { it.copy(title = v) }
        }

        fun setDescription(v: String) {
            _uiState.update { it.copy(description = v) }
        }

        fun setPriority(v: TaskPriority) {
            _uiState.update { it.copy(priority = v) }
        }

        fun setDeadline(v: Long?) {
            _uiState.update { it.copy(deadline = v) }
        }

        fun clearMessages() {
            _uiState.update { it.copy(error = null, successMessage = null) }
        }

        fun saveTask() {
            val s = _uiState.value
            if (s.title.isBlank()) return
            viewModelScope.launch {
                try {
                    if (s.editingTask != null) {
                        repository.updateTask(
                            s.editingTask.copy(
                                title = s.title,
                                description = s.description,
                                priority = s.priority,
                                deadline = s.deadline,
                            ),
                        )
                    } else {
                        repository.addTask(
                            Task(
                                title = s.title,
                                description = s.description,
                                priority = s.priority,
                                deadline = s.deadline,
                            ),
                        )
                    }
                    hideDialog()
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Failed: ${e.message}") }
                }
            }
        }

        /**
         * Convenience overload called by [SuperAddBottomSheet] which carries its own
         * local form state and delivers the final values in one shot.
         */
        fun saveTaskWith(
            title: String,
            description: String,
            priority: TaskPriority,
            deadline: Long?,
            reminderOffsets: List<Int> = emptyList(),
            alarmEnabled: Boolean = false,
        ) {
            if (title.isBlank()) return
            viewModelScope.launch {
                try {
                    val editing = _uiState.value.editingTask
                    if (editing != null) {
                        repository.updateTask(
                            editing.copy(
                                title = title,
                                description = description,
                                priority = priority,
                                deadline = deadline,
                                reminderOffsets = reminderOffsets,
                                alarmEnabled = alarmEnabled,
                            ),
                        )
                    } else {
                        repository.addTask(
                            Task(
                                title = title,
                                description = description,
                                priority = priority,
                                deadline = deadline,
                                reminderOffsets = reminderOffsets,
                                alarmEnabled = alarmEnabled,
                            ),
                        )
                    }
                    hideDialog()
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Failed: ${e.message}") }
                }
            }
        }

        fun completeTask(task: Task) {
            viewModelScope.launch {
                try {
                    repository.completeTask(task)
                } catch (
                    e: Exception,
                ) {
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }

        fun deleteTask(task: Task) {
            viewModelScope.launch {
                try {
                    repository.deleteTask(task)
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }

        fun undoComplete(task: Task) {
            viewModelScope.launch {
                repository.updateTask(task.copy(status = TaskStatus.PENDING, completedAt = null))
            }
        }

        private fun loadTasks() {
            repository.getPendingTasks()
                .onEach { _uiState.update { s -> s.copy(pendingTasks = it, isLoading = false) } }
                .catch { _uiState.update { it.copy(isLoading = false) } }
                .launchIn(viewModelScope)
            repository.getCompletedTasks()
                .onEach { _uiState.update { s -> s.copy(completedTasks = it) } }
                .catch { }
                .launchIn(viewModelScope)
        }
    }
