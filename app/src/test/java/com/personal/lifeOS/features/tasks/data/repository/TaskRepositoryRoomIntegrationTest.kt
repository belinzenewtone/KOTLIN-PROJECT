package com.personal.lifeOS.features.tasks.data.repository

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.lifeOS.core.database.LifeOSDatabase
import com.personal.lifeOS.core.database.entity.SyncJobEntity
import com.personal.lifeOS.core.notifications.TaskReminderScheduler
import com.personal.lifeOS.core.preferences.AppSettingsStore
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.core.sync.SyncMutationEnqueuer
import com.personal.lifeOS.core.sync.SyncQueueStore
import com.personal.lifeOS.core.sync.model.SyncJobType
import com.personal.lifeOS.features.tasks.domain.model.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TaskRepositoryRoomIntegrationTest {
    @Test
    fun `task writes reconcile into Room flows and enqueue sync operations`() =
        runTest {
            val fixture = createFixture(this)
            try {
                val taskId =
                    fixture.repository.addTask(
                        Task(
                            title = "Submit project report",
                            description = "Final weekly submission",
                        ),
                    )

                val pending = fixture.repository.getPendingTasks().first()
                assertEquals(1, pending.size)
                assertEquals(taskId, pending.first().id)

                fixture.repository.completeTask(pending.first())
                assertTrue(fixture.repository.getPendingTasks().first().isEmpty())
                val completed = fixture.repository.getCompletedTasks().first()
                assertEquals(1, completed.size)

                fixture.repository.deleteTask(completed.first())
                assertTrue(fixture.repository.getCompletedTasks().first().isEmpty())

                val taskOps = fixture.queueStore.enqueued.filter { it.entityType == "task" }
                assertEquals(3, taskOps.size)
                assertEquals(SyncJobType.PUSH_ALL, taskOps[0].type)
                assertTrue(taskOps[0].payload.contains("\"operation\":\"UPSERT\""))
                assertTrue(taskOps[1].payload.contains("\"operation\":\"UPSERT\""))
                assertTrue(taskOps[2].payload.contains("\"operation\":\"DELETE\""))
            } finally {
                fixture.authSessionStore.clearSession()
                fixture.database.close()
            }
        }

    @Test
    fun `task repository remains user scoped after session switch`() =
        runTest {
            val fixture = createFixture(this)
            try {
                fixture.repository.addTask(
                    Task(
                        title = "User scoped task",
                        description = "Owned by primary user",
                    ),
                )
                fixture.authSessionStore.saveSession("token-2", "other-user")

                assertTrue(fixture.repository.getPendingTasks().first().isEmpty())
                assertEquals(1, fixture.database.taskDao().getAllForSync(fixture.userId).size)
            } finally {
                fixture.authSessionStore.clearSession()
                fixture.database.close()
            }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun createFixture(scope: TestScope): TaskRepositoryFixture {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database =
        Room.inMemoryDatabaseBuilder(context, LifeOSDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    val authSessionStore = AuthSessionStore(context)
    val userId = "task-user"
    authSessionStore.saveSession("token", userId)

    val settingsStore =
        AppSettingsStore(
            dataStore =
                PreferenceDataStoreFactory.create(scope = scope.backgroundScope) {
                    File(Files.createTempDirectory("task-settings").toFile(), "settings.preferences_pb")
                },
        )

    val queueStore = TaskFakeSyncQueueStore()
    val repository =
        TaskRepositoryImpl(
            taskDao = database.taskDao(),
            authSessionStore = authSessionStore,
            reminderScheduler = TaskReminderScheduler(context, settingsStore),
            syncMutationEnqueuer = SyncMutationEnqueuer(queueStore),
        )

    return TaskRepositoryFixture(
        repository = repository,
        authSessionStore = authSessionStore,
        queueStore = queueStore,
        database = database,
        userId = userId,
    )
}

private data class TaskRepositoryFixture(
    val repository: TaskRepositoryImpl,
    val authSessionStore: AuthSessionStore,
    val queueStore: TaskFakeSyncQueueStore,
    val database: LifeOSDatabase,
    val userId: String,
)

private data class TaskEnqueueEntry(
    val type: SyncJobType,
    val entityType: String,
    val entityId: String,
    val payload: String,
)

private class TaskFakeSyncQueueStore : SyncQueueStore {
    val enqueued = mutableListOf<TaskEnqueueEntry>()

    override suspend fun enqueue(
        type: SyncJobType,
        entityType: String,
        entityId: String,
        payload: String,
    ): Long {
        enqueued += TaskEnqueueEntry(type, entityType, entityId, payload)
        return enqueued.size.toLong()
    }

    override suspend fun dueJobs(limit: Int): List<SyncJobEntity> = emptyList()

    override suspend fun markSyncing(job: SyncJobEntity) = Unit

    override suspend fun markSynced(job: SyncJobEntity) = Unit

    override suspend fun markFailed(
        job: SyncJobEntity,
        error: String?,
        nextRunAt: Long,
    ) = Unit

    override suspend fun pruneSynced(olderThan: Long): Int = 0

    override fun observeJobs(): Flow<List<SyncJobEntity>> = flowOf(emptyList())
}
