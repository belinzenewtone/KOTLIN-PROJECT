package com.personal.lifeOS.features.tasks.data.repository

import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.entity.TaskEntity
import com.personal.lifeOS.features.tasks.domain.model.Task
import com.personal.lifeOS.features.tasks.domain.model.TaskPriority
import com.personal.lifeOS.features.tasks.domain.model.TaskStatus
import com.personal.lifeOS.features.tasks.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { it.map { e -> e.toDomain() } }
    }

    override fun getPendingTasks(): Flow<List<Task>> {
        return taskDao.getPendingTasks().map { it.map { e -> e.toDomain() } }
    }

    override fun getCompletedTasks(): Flow<List<Task>> {
        return taskDao.getCompletedTasks().map { it.map { e -> e.toDomain() } }
    }

    override fun getPendingCount(): Flow<Int> {
        return taskDao.getPendingCount()
    }

    override suspend fun addTask(task: Task): Long {
        return taskDao.insert(task.toEntity())
    }

    override suspend fun updateTask(task: Task) {
        taskDao.update(task.toEntity())
    }

    override suspend fun deleteTask(task: Task) {
        taskDao.delete(task.toEntity())
    }

    override suspend fun completeTask(task: Task) {
        taskDao.update(
            task.copy(
                status = TaskStatus.COMPLETED,
                completedAt = System.currentTimeMillis()
            ).toEntity()
        )
    }

    override suspend fun getById(id: Long): Task? {
        return taskDao.getById(id)?.toDomain()
    }
}

fun TaskEntity.toDomain(): Task {
    return Task(
        id = id, title = title, description = description,
        priority = try { TaskPriority.valueOf(priority) } catch (_: Exception) { TaskPriority.MEDIUM },
        deadline = deadline,
        status = try { TaskStatus.valueOf(status) } catch (_: Exception) { TaskStatus.PENDING },
        completedAt = completedAt, createdAt = createdAt
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id, title = title, description = description,
        priority = priority.name, deadline = deadline,
        status = status.name, completedAt = completedAt, createdAt = createdAt
    )
}
