package com.personal.lifeOS.core.sync

import com.personal.lifeOS.core.database.entity.SyncJobEntity
import com.personal.lifeOS.core.sync.model.SyncJobType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncMutationEnqueuerTest {
    @Test
    fun `enqueueUpsert uses PUSH_ALL and upsert payload`() =
        runTest {
            val queueStore = FakeQueueStore()
            val enqueuer = SyncMutationEnqueuer(queueStore)

            enqueuer.enqueueUpsert(
                entityType = "task",
                entityId = "88",
            )

            val call = queueStore.calls.single()
            assertEquals(SyncJobType.PUSH_ALL, call.type)
            assertEquals("task", call.entityType)
            assertEquals("88", call.entityId)
            assertEquals(true, call.payload.contains("\"operation\":\"UPSERT\""))
        }

    @Test
    fun `enqueueDelete uses PUSH_ALL and delete payload`() =
        runTest {
            val queueStore = FakeQueueStore()
            val enqueuer = SyncMutationEnqueuer(queueStore)

            enqueuer.enqueueDelete(
                entityType = "event",
                entityId = "3",
            )

            val call = queueStore.calls.single()
            assertEquals(SyncJobType.PUSH_ALL, call.type)
            assertEquals("event", call.entityType)
            assertEquals("3", call.entityId)
            assertEquals(true, call.payload.contains("\"operation\":\"DELETE\""))
        }
}

private class FakeQueueStore : SyncQueueStore {
    data class Call(
        val type: SyncJobType,
        val entityType: String,
        val entityId: String,
        val payload: String,
    )

    val calls = mutableListOf<Call>()

    override suspend fun enqueue(
        type: SyncJobType,
        entityType: String,
        entityId: String,
        payload: String,
    ): Long {
        calls += Call(type, entityType, entityId, payload)
        return calls.size.toLong()
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
