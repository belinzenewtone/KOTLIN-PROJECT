package com.personal.lifeOS.features.tasks.presentation

sealed interface TasksUiEvent {
    data object AddTask : TasksUiEvent

    data class EditTask(val taskId: Long) : TasksUiEvent

    data class CompleteTask(val taskId: Long) : TasksUiEvent

    data class DeleteTask(val taskId: Long) : TasksUiEvent

    data object DismissDialog : TasksUiEvent
}
