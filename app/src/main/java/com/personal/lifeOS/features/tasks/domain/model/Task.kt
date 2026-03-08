package com.personal.lifeOS.features.tasks.domain.model

data class Task(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val priority: TaskPriority = TaskPriority.NEUTRAL,
    val deadline: Long? = null,
    val status: TaskStatus = TaskStatus.PENDING,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class TaskPriority(val label: String) {
    NEUTRAL("Neutral"),
    IMPORTANT("Important"),
    URGENT("Urgent"),
}

enum class TaskStatus(val label: String) {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
}
