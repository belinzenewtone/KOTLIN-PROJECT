package com.personal.lifeOS.features.expenses.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.lifeOS.core.database.LifeOSDatabase
import com.personal.lifeOS.core.database.entity.SyncJobEntity
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.core.sync.SyncMutationEnqueuer
import com.personal.lifeOS.core.sync.SyncQueueStore
import com.personal.lifeOS.core.sync.model.SyncJobType
import com.personal.lifeOS.features.expenses.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ExpenseRepositoryRoomIntegrationTest {
    private lateinit var db: LifeOSDatabase
    private lateinit var authSessionStore: AuthSessionStore
    private lateinit var syncQueueStore: FakeSyncQueueStore
    private lateinit var repository: ExpenseRepositoryImpl

    private val userId = "room-user"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, LifeOSDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        authSessionStore = AuthSessionStore(context)
        authSessionStore.saveSession("token", userId)
        syncQueueStore = FakeSyncQueueStore()
        repository =
            ExpenseRepositoryImpl(
                transactionDao = db.transactionDao(),
                merchantCategoryDao = db.merchantCategoryDao(),
                authSessionStore = authSessionStore,
                syncMutationEnqueuer = SyncMutationEnqueuer(syncQueueStore),
            )
    }

    @After
    fun tearDown() {
        authSessionStore.clearSession()
        db.close()
    }

    @Test
    fun `add transaction persists to Room observed flow and enqueues sync upsert`() =
        runTest {
            val now = 1_700_000_000_000L
            val transaction =
                Transaction(
                    amount = 1250.0,
                    merchant = "NAIVAS WESTLANDS",
                    category = "Groceries",
                    date = now,
                    source = "MANUAL",
                    transactionType = "PAID",
                )

            val id = repository.addTransaction(transaction)

            val observed = repository.getAllTransactions().first()
            assertEquals(1, observed.size)
            assertEquals(id, observed.first().id)
            assertEquals("NAIVAS WESTLANDS", observed.first().merchant)
            assertEquals("MANUAL", observed.first().source)

            val entity = db.transactionDao().getById(id, userId)
            assertNotNull(entity)
            val persisted = requireNotNull(entity)
            assertEquals(userId, persisted.userId)
            assertEquals("LOCAL_ONLY", persisted.syncState)

            assertEquals(1, syncQueueStore.enqueued.size)
            val enqueue = syncQueueStore.enqueued.first()
            assertEquals(SyncJobType.PUSH_ALL, enqueue.type)
            assertEquals("transaction", enqueue.entityType)
            assertEquals(id.toString(), enqueue.entityId)
            assertTrue(enqueue.payload.contains("\"operation\":\"UPSERT\""))
        }

    @Test
    fun `import from sms reconciles into Room and duplicate is ignored`() =
        runTest {
            val sms =
                "QK99887766 Confirmed. Ksh540.00 paid to NAIVAS WESTLANDS on 18/3/26 at 7:20 PM. " +
                    "New M-PESA balance is Ksh1,200.00."

            val first = repository.importFromSms(sms)
            val second = repository.importFromSms(sms)

            assertNotNull(first)
            assertNull(second)

            val observed = repository.getAllTransactions().first()
            assertEquals(1, observed.size)
            val transaction = observed.first()
            assertEquals("QK99887766", transaction.mpesaCode)
            assertEquals(540.0, transaction.amount, 0.0)

            val transactionUpserts =
                syncQueueStore.enqueued.filter { entry ->
                    entry.entityType == "transaction" && entry.payload.contains("\"operation\":\"UPSERT\"")
                }
            assertEquals(1, transactionUpserts.size)
        }

    @Test
    fun `Room-observed transactions remain user-scoped when active session changes`() =
        runTest {
            val id =
                repository.addTransaction(
                    Transaction(
                        amount = 320.0,
                        merchant = "JAVA HOUSE",
                        category = "Dining",
                        date = 1_700_100_000_000L,
                        source = "MANUAL",
                        transactionType = "PAID",
                    ),
                )
            assertNotNull(db.transactionDao().getById(id, userId))

            authSessionStore.saveSession("token-2", "different-user")

            val observedAfterSwitch = repository.getAllTransactions().first()
            assertTrue(observedAfterSwitch.isEmpty())
            assertEquals(1, db.transactionDao().getAllForSync(userId).size)
        }
}

private data class EnqueueEntry(
    val type: SyncJobType,
    val entityType: String,
    val entityId: String,
    val payload: String,
)

private class FakeSyncQueueStore : SyncQueueStore {
    val enqueued = mutableListOf<EnqueueEntry>()

    override suspend fun enqueue(
        type: SyncJobType,
        entityType: String,
        entityId: String,
        payload: String,
    ): Long {
        enqueued += EnqueueEntry(type = type, entityType = entityType, entityId = entityId, payload = payload)
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
