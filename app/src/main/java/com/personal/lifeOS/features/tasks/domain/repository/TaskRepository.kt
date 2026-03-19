package com.personal.lifeOS.features.tasks.domain.repository

import com.personal.lifeOS.features.tasks.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>

    fun getPendingTasks(): Flow<List<Task>>

    fun getCompletedTasks(): Flow<List<Task>>

    fun getPendingCount(): Flow<Int>

    suspend fun addTask(task: Task): Long

    suspend fun updateTask(task: Task)

    suspend fun deleteTask(task: Task)

    suspend fun completeTask(task: Task)

    suspend fun getById(id: Long): Task?
}
