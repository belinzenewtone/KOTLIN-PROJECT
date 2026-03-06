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
    val showCompleted: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showAddDialog: Boolean = false
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    init {
        loadPendingTasks()
        loadCompletedTasks()
    }

    fun toggleShowCompleted() {
        _uiState.update { it.copy(showCompleted = !it.showCompleted) }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true) }
    }

    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun addTask(title: String, description: String, priority: TaskPriority) {
        viewModelScope.launch {
            try {
                repository.addTask(
                    Task(title = title, description = description, priority = priority)
                )
                _uiState.update { it.copy(showAddDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.completeTask(task)
            } catch (e: Exception) {
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

    private fun loadPendingTasks() {
        repository.getPendingTasks()
            .onEach { tasks ->
                _uiState.update { it.copy(pendingTasks = tasks, isLoading = false) }
            }
            .catch { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadCompletedTasks() {
        repository.getCompletedTasks()
            .onEach { tasks ->
                _uiState.update { it.copy(completedTasks = tasks) }
            }
            .catch { /* silent */ }
            .launchIn(viewModelScope)
    }
}
