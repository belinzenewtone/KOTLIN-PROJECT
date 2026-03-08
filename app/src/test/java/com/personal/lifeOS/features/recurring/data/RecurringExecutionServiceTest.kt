package com.personal.lifeOS.features.recurring.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.lifeOS.core.database.LifeOSDatabase
import com.personal.lifeOS.core.database.entity.RecurringRuleEntity
import com.personal.lifeOS.core.security.AuthSessionStore
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
class RecurringExecutionServiceTest {
    private lateinit var db: LifeOSDatabase
    private lateinit var authSessionStore: AuthSessionStore
    private lateinit var service: RecurringExecutionService

    private val userId = "test-user"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, LifeOSDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        authSessionStore = AuthSessionStore(context)
        authSessionStore.saveSession("token", userId)
        service =
            RecurringExecutionService(
                database = db,
                recurringRuleDao = db.recurringRuleDao(),
                transactionDao = db.transactionDao(),
                incomeDao = db.incomeDao(),
                taskDao = db.taskDao(),
                authSessionStore = authSessionStore,
            )
    }

    @After
    fun tearDown() {
        authSessionStore.clearSession()
        db.close()
    }

    @Test
    fun `processDueRules materializes due expense rule exactly once`() =
        runTest {
            val initialRunAt = System.currentTimeMillis() - 1_000L
            db.recurringRuleDao().insert(
                RecurringRuleEntity(
                    id = 1L,
                    title = "Rent",
                    type = "EXPENSE",
                    cadence = "DAILY",
                    nextRunAt = initialRunAt,
                    amount = 2500.0,
                    enabled = true,
                    userId = userId,
                ),
            )

            val first = service.processDueRules(maxRules = 10)
            val second = service.processDueRules(maxRules = 10)

            assertEquals(1, first.processedRules)
            assertEquals(1, first.createdTransactions)
            assertEquals(0, first.createdIncomes)
            assertEquals(0, first.createdTasks)

            assertEquals(0, second.processedRules)
            assertEquals(0, second.createdTransactions)

            val allTransactions = db.transactionDao().getAllForSync(userId)
            assertEquals(1, allTransactions.size)

            val updatedRule = db.recurringRuleDao().getAllForSync(userId).first()
            assertTrue(updatedRule.nextRunAt > initialRunAt)
        }

    @Test
    fun `processDueRules skips when no active user session`() =
        runTest {
            authSessionStore.clearSession()

            val result = service.processDueRules(maxRules = 10)

            assertEquals(0, result.processedRules)
            assertEquals(0, result.createdTransactions)
            assertEquals(0, result.createdIncomes)
            assertEquals(0, result.createdTasks)
            assertEquals(0, result.skippedRules)
        }
}
