package com.personal.lifeOS.core.sync

import com.personal.lifeOS.core.database.dao.SyncJobDao
import com.personal.lifeOS.core.database.entity.SyncJobEntity
import com.personal.lifeOS.core.sync.model.SyncJobType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomSyncQueueStoreTest {
    @Test
    fun `enqueue deduplicates active job for same entity key`() =
        runTest {
            val dao = FakeSyncJobDao()
            val store = RoomSyncQueueStore(dao)

            store.enqueue(
                type = SyncJobType.PUSH_ALL,
                entityType = "task",
                entityId = "42",
                payload = "{\"operation\":\"UPSERT\"}",
            )

            store.enqueue(
                type = SyncJobType.PUSH_ALL,
                entityType = "task",
                entityId = "42",
                payload = "{\"operation\":\"DELETE\"}",
            )

            val jobs = dao.snapshot()
            assertEquals(1, jobs.size)
            assertEquals("{\"operation\":\"DELETE\"}", jobs.first().payload)
            assertEquals("QUEUED", jobs.first().status)
        }

    @Test
    fun `enqueue resets failed attempt on existing job`() =
        runTest {
            val dao = FakeSyncJobDao()
            val store = RoomSyncQueueStore(dao)

            val id =
                store.enqueue(
                    type = SyncJobType.PUSH_ALL,
                    entityType = "budget",
                    entityId = "11",
                    payload = "{}",
                )

            dao.update(
                dao.snapshot().first { it.id == id }.copy(
                    status = "FAILED",
                    attemptCount = 4,
                    lastError = "timeout",
                ),
            )

            store.enqueue(
                type = SyncJobType.PUSH_ALL,
                entityType = "budget",
                entityId = "11",
                payload = "{\"operation\":\"UPSERT\"}",
            )

            val updated = dao.snapshot().first { it.id == id }
            assertEquals("QUEUED", updated.status)
            assertEquals(0, updated.attemptCount)
            assertNull(updated.lastError)
        }
}

private class FakeSyncJobDao : SyncJobDao {
    private val records = mutableListOf<SyncJobEntity>()
    private val state = MutableStateFlow<List<SyncJobEntity>>(emptyList())
    private var nextId = 1L

    override suspend fun insert(job: SyncJobEntity): Long {
        val assigned = if (job.id > 0L) job else job.copy(id = nextId++)
        records += assigned
        publish()
        return assigned.id
    }

    override suspend fun insertAll(jobs: List<SyncJobEntity>) {
        jobs.forEach { insert(it) }
    }

    override suspend fun update(job: SyncJobEntity) {
        val index = records.indexOfFirst { it.id == job.id }
        if (index >= 0) {
            records[index] = job
            publish()
        }
    }

    override suspend fun findActiveJob(
        jobType: String,
        entityType: String,
        entityId: String,
    ): SyncJobEntity? {
        return records
            .asSequence()
            .filter {
                it.jobType == jobType &&
                    it.entityType == entityType &&
                    it.entityId == entityId &&
                    it.status in setOf("QUEUED", "FAILED", "SYNCING")
            }
            .maxByOrNull { it.updatedAt }
    }

    override suspend fun getDueJobs(
        now: Long,
        limit: Int,
    ): List<SyncJobEntity> {
        return records
            .filter { it.status in setOf("QUEUED", "FAILED") && it.nextRunAt <= now }
            .sortedBy { it.nextRunAt }
            .take(limit)
    }

    override fun observeAll(): Flow<List<SyncJobEntity>> = state

    override suspend fun pruneSynced(olderThan: Long): Int {
        val before = records.size
        records.removeAll { it.status == "SYNCED" && it.updatedAt < olderThan }
        publish()
        return before - records.size
    }

    override suspend fun getPendingCount(): Int {
        return records.count { it.status in setOf("QUEUED", "FAILED") }
    }

    fun snapshot(): List<SyncJobEntity> = records.toList()

    private fun publish() {
        state.value = records.toList()
    }
}
