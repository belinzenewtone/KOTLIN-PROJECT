package com.personal.lifeOS.features.search.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.lifeOS.core.database.LifeOSDatabase
import com.personal.lifeOS.core.database.entity.BudgetEntity
import com.personal.lifeOS.core.database.entity.EventEntity
import com.personal.lifeOS.core.database.entity.IncomeEntity
import com.personal.lifeOS.core.database.entity.RecurringRuleEntity
import com.personal.lifeOS.core.database.entity.TaskEntity
import com.personal.lifeOS.core.database.entity.TransactionEntity
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.features.search.domain.model.SearchResult
import com.personal.lifeOS.features.search.domain.model.SearchSource
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
class SearchRepositoryRoomIntegrationTest {
    private lateinit var db: LifeOSDatabase
    private lateinit var authSessionStore: AuthSessionStore
    private lateinit var repository: SearchRepositoryImpl

    private val userA = "search-user-a"
    private val userB = "search-user-b"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, LifeOSDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        authSessionStore = AuthSessionStore(context)
        authSessionStore.saveSession("token-a", userA)

        repository =
            SearchRepositoryImpl(
                transactionDao = db.transactionDao(),
                taskDao = db.taskDao(),
                eventDao = db.eventDao(),
                budgetDao = db.budgetDao(),
                incomeDao = db.incomeDao(),
                recurringRuleDao = db.recurringRuleDao(),
                authSessionStore = authSessionStore,
            )
    }

    @After
    fun tearDown() {
        authSessionStore.clearSession()
        db.close()
    }

    @Test
    fun `search reads only user-scoped Room data and returns results ordered by recency`() =
        runTest {
            seedUserAAlphaData()
            seedUserBNoiseData()

            val results = repository.search(query = "Alpha", limitPerSource = 10)

            assertSortedCrossDomainResults(results)
            assertTrue(results.none { it.id.contains("101") })
        }

    @Test
    fun `search returns empty list when user session is missing`() =
        runTest {
            authSessionStore.clearSession()

            val results = repository.search(query = "Alpha", limitPerSource = 10)

            assertTrue(results.isEmpty())
        }

    private suspend fun seedUserAAlphaData() {
        db.transactionDao().insert(
            TransactionEntity(
                id = 1L,
                amount = 120.0,
                merchant = "Alpha Store",
                category = "Groceries",
                date = 1_000L,
                userId = userA,
            ),
        )
        db.taskDao().insert(
            TaskEntity(
                id = 2L,
                title = "Alpha Task",
                description = "Weekly planning",
                createdAt = 1_500L,
                userId = userA,
            ),
        )
        db.eventDao().insert(
            EventEntity(
                id = 3L,
                title = "Alpha Event",
                description = "Sync review",
                date = 1_800L,
                userId = userA,
            ),
        )
        db.budgetDao().insert(
            BudgetEntity(
                id = 4L,
                category = "ALPHA",
                limitAmount = 4_000.0,
                createdAt = 2_000L,
                userId = userA,
            ),
        )
        db.recurringRuleDao().insert(
            RecurringRuleEntity(
                id = 5L,
                title = "Alpha Recurring",
                type = "TASK",
                cadence = "WEEKLY",
                nextRunAt = 2_100L,
                userId = userA,
            ),
        )
        db.incomeDao().insert(
            IncomeEntity(
                id = 6L,
                amount = 3_500.0,
                source = "Alpha Salary",
                date = 2_200L,
                userId = userA,
            ),
        )
    }

    private suspend fun seedUserBNoiseData() {
        db.transactionDao().insert(
            TransactionEntity(
                id = 101L,
                amount = 999.0,
                merchant = "Alpha Foreign",
                category = "Other",
                date = 9_999L,
                userId = userB,
            ),
        )
    }

    private fun assertSortedCrossDomainResults(results: List<SearchResult>) {
        assertEquals(6, results.size)
        assertEquals(
            listOf("income-6", "rule-5", "budget-4", "event-3", "task-2", "tx-1"),
            results.map { it.id },
        )
        assertEquals(
            listOf(
                SearchSource.INCOME,
                SearchSource.RECURRING_RULE,
                SearchSource.BUDGET,
                SearchSource.EVENT,
                SearchSource.TASK,
                SearchSource.TRANSACTION,
            ),
            results.map { it.source },
        )
    }
}
