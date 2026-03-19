package com.personal.lifeOS.features.finance.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.lifeOS.core.database.LifeOSDatabase
import com.personal.lifeOS.core.database.entity.SyncJobEntity
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.core.sync.SyncMutationEnqueuer
import com.personal.lifeOS.core.sync.SyncQueueStore
import com.personal.lifeOS.core.sync.model.SyncJobType
import com.personal.lifeOS.features.budget.data.repository.BudgetRepositoryImpl
import com.personal.lifeOS.features.budget.domain.model.Budget
import com.personal.lifeOS.features.budget.domain.model.BudgetPeriod
import com.personal.lifeOS.features.income.data.repository.IncomeRepositoryImpl
import com.personal.lifeOS.features.income.domain.model.IncomeRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class FinanceRoomSourceOfTruthIntegrationTest {
    private lateinit var db: LifeOSDatabase
    private lateinit var authSessionStore: AuthSessionStore
    private lateinit var queueStore: FinanceFakeSyncQueueStore
    private lateinit var budgetRepository: BudgetRepositoryImpl
    private lateinit var incomeRepository: IncomeRepositoryImpl

    private val userId = "finance-user"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, LifeOSDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        authSessionStore = AuthSessionStore(context)
        authSessionStore.saveSession("token", userId)

        queueStore = FinanceFakeSyncQueueStore()
        val syncMutationEnqueuer = SyncMutationEnqueuer(queueStore)

        budgetRepository =
            BudgetRepositoryImpl(
                budgetDao = db.budgetDao(),
                authSessionStore = authSessionStore,
                syncMutationEnqueuer = syncMutationEnqueuer,
            )
        incomeRepository =
            IncomeRepositoryImpl(
                incomeDao = db.incomeDao(),
                authSessionStore = authSessionStore,
                syncMutationEnqueuer = syncMutationEnqueuer,
            )
    }

    @After
    fun tearDown() {
        authSessionStore.clearSession()
        db.close()
    }

    @Test
    fun `budget writes reconcile into Room observed flow and emit sync jobs`() =
        runTest {
            val budgetId =
                budgetRepository.addBudget(
                    Budget(
                        category = " groceries ",
                        limitAmount = 12_500.0,
                        period = BudgetPeriod.MONTHLY,
                    ),
                )

            var observedBudgets = budgetRepository.getBudgets().first()
            assertEquals(1, observedBudgets.size)
            assertEquals("GROCERIES", observedBudgets.first().category)
            assertEquals(12_500.0, observedBudgets.first().limitAmount, 0.0)

            budgetRepository.updateBudget(
                observedBudgets.first().copy(limitAmount = 10_000.0),
            )
            observedBudgets = budgetRepository.getBudgets().first()
            assertEquals(10_000.0, observedBudgets.first().limitAmount, 0.0)

            val budgetOps = queueStore.enqueued.filter { it.entityType == "budget" }
            assertEquals(2, budgetOps.size)
            assertEquals(SyncJobType.PUSH_ALL, budgetOps[0].type)
            assertEquals(budgetId.toString(), budgetOps[0].entityId)
            assertTrue(budgetOps[0].payload.contains("\"operation\":\"UPSERT\""))
            assertTrue(budgetOps[1].payload.contains("\"operation\":\"UPSERT\""))
        }

    @Test
    fun `income add and delete reconcile into Room and emit sync jobs`() =
        runTest {
            val incomeId =
                incomeRepository.addIncome(
                    IncomeRecord(
                        amount = 3_500.0,
                        source = " Salary ",
                        note = "March payout",
                        date = 1_700_100_000_000L,
                    ),
                )

            var incomes = incomeRepository.getIncomes().first()
            assertEquals(1, incomes.size)
            assertEquals("Salary", incomes.first().source)
            assertEquals(3_500.0, incomes.first().amount, 0.0)

            incomeRepository.deleteIncome(incomeId)
            incomes = incomeRepository.getIncomes().first()
            assertTrue(incomes.isEmpty())

            val incomeOps = queueStore.enqueued.filter { it.entityType == "income" }
            assertEquals(2, incomeOps.size)
            assertEquals(incomeId.toString(), incomeOps[0].entityId)
            assertTrue(incomeOps[0].payload.contains("\"operation\":\"UPSERT\""))
            assertTrue(incomeOps[1].payload.contains("\"operation\":\"DELETE\""))
        }

    @Test
    fun `budget and income observation is user-scoped after session switch`() =
        runTest {
            budgetRepository.addBudget(
                Budget(
                    category = "Transport",
                    limitAmount = 5_000.0,
                    period = BudgetPeriod.MONTHLY,
                ),
            )
            incomeRepository.addIncome(
                IncomeRecord(
                    amount = 2_000.0,
                    source = "Freelance",
                    note = "Client project",
                ),
            )

            authSessionStore.saveSession("token-2", "different-user")

            val budgetsAfterSwitch = budgetRepository.getBudgets().first()
            val incomesAfterSwitch = incomeRepository.getIncomes().first()
            assertTrue(budgetsAfterSwitch.isEmpty())
            assertTrue(incomesAfterSwitch.isEmpty())

            assertEquals(1, db.budgetDao().getAllForSync(userId).size)
            assertEquals(1, db.incomeDao().getAllForSync(userId).size)
        }
}

private data class FinanceEnqueueEntry(
    val type: SyncJobType,
    val entityType: String,
    val entityId: String,
    val payload: String,
)

private class FinanceFakeSyncQueueStore : SyncQueueStore {
    val enqueued = mutableListOf<FinanceEnqueueEntry>()

    override suspend fun enqueue(
        type: SyncJobType,
        entityType: String,
        entityId: String,
        payload: String,
    ): Long {
        enqueued +=
            FinanceEnqueueEntry(
                type = type,
                entityType = entityType,
                entityId = entityId,
                payload = payload,
            )
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
