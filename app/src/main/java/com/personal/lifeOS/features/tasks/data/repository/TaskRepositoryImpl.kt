package com.personal.lifeOS.features.tasks.data.repository

import com.personal.lifeOS.core.database.LocalIdGenerator
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.entity.TaskEntity
import com.personal.lifeOS.core.notifications.TaskReminderScheduler
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.core.sync.SyncMutationEnqueuer
import com.personal.lifeOS.features.tasks.domain.model.Task
import com.personal.lifeOS.features.tasks.domain.model.TaskPriority
import com.personal.lifeOS.features.tasks.domain.model.TaskStatus
import com.personal.lifeOS.features.tasks.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl
    @Inject
    constructor(
        private val taskDao: TaskDao,
        private val authSessionStore: AuthSessionStore,
        private val reminderScheduler: TaskReminderScheduler,
        private val syncMutationEnqueuer: SyncMutationEnqueuer,
    ) : TaskRepository {
        private fun activeUserId(): String = authSessionStore.getUserId()

        override fun getAllTasks(): Flow<List<Task>> {
            return taskDao.getAllTasks(activeUserId()).map { it.map { e -> e.toDomain() } }
        }

        override fun getPendingTasks(): Flow<List<Task>> {
            return taskDao.getPendingTasks(activeUserId()).map { it.map { e -> e.toDomain() } }
        }

        override fun getCompletedTasks(): Flow<List<Task>> {
            return taskDao.getCompletedTasks(activeUserId()).map { it.map { e -> e.toDomain() } }
        }

        override fun getPendingCount(): Flow<Int> {
            return taskDao.getPendingCount(activeUserId())
        }

        override suspend fun addTask(task: Task): Long {
            val userId = activeUserId()
            val stableId = if (task.id > 0L) task.id else LocalIdGenerator.nextId()
            val storedTask = task.copy(id = stableId)
            taskDao.insert(
                storedTask.toEntity().copy(
                    id = stableId,
                    userId = userId,
                ),
            )
            scheduleReminderIfNeeded(storedTask, userId)
            syncMutationEnqueuer.enqueueUpsert(
                entityType = "task",
                entityId = stableId.toString(),
            )
            return stableId
        }

        override suspend fun updateTask(task: Task) {
            val userId = activeUserId()
            taskDao.update(task.toEntity().copy(userId = userId))
            scheduleReminderIfNeeded(task, userId)
            syncMutationEnqueuer.enqueueUpsert(
                entityType = "task",
                entityId = task.id.toString(),
            )
        }

        override suspend fun deleteTask(task: Task) {
            val userId = activeUserId()
            reminderScheduler.cancelTaskReminder(task.id, userId)
            taskDao.delete(task.toEntity().copy(userId = userId))
            syncMutationEnqueuer.enqueueDelete(
                entityType = "task",
                entityId = task.id.toString(),
            )
        }

        override suspend fun completeTask(task: Task) {
            val userId = activeUserId()
            reminderScheduler.cancelTaskReminder(task.id, userId)
            taskDao.update(
                task.copy(
                    status = TaskStatus.COMPLETED,
                    completedAt = System.currentTimeMillis(),
                ).toEntity().copy(userId = userId),
            )
            syncMutationEnqueuer.enqueueUpsert(
                entityType = "task",
                entityId = task.id.toString(),
            )
        }

        override suspend fun getById(id: Long): Task? {
            return taskDao.getById(id, activeUserId())?.toDomain()
        }

        private suspend fun scheduleReminderIfNeeded(
            task: Task,
            userId: String,
        ) {
            if (task.status == TaskStatus.COMPLETED || task.deadline == null) {
                reminderScheduler.cancelTaskReminder(task.id, userId)
                return
            }
            reminderScheduler.scheduleTaskReminder(task = task, userId = userId)
        }
    }

fun TaskEntity.toDomain(): Task {
    return Task(
        id = id,
        title = title,
        description = description,
        priority =
            try {
                TaskPriority.valueOf(priority)
            } catch (_: Exception) {
                TaskPriority.NEUTRAL
            },
        deadline = deadline,
        status =
            try {
                TaskStatus.valueOf(status)
            } catch (_: Exception) {
                TaskStatus.PENDING
            },
        completedAt = completedAt,
        createdAt = createdAt,
        reminderOffsets = if (reminderOffsets.isBlank()) emptyList()
            else reminderOffsets.split(",").mapNotNull { it.trim().toIntOrNull() },
        alarmEnabled = alarmEnabled,
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        priority = priority.name,
        deadline = deadline,
        status = status.name,
        completedAt = completedAt,
        createdAt = createdAt,
        reminderOffsets = reminderOffsets.joinToString(","),
        alarmEnabled = alarmEnabled,
    )
}
