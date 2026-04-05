package com.personal.lifeOS.features.expenses.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.lifeOS.core.database.LifeOSDatabase
import com.personal.lifeOS.core.security.AuthSessionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class FulizaLoanRepositoryImplTest {
    private lateinit var db: LifeOSDatabase
    private lateinit var authSessionStore: AuthSessionStore
    private lateinit var repository: FulizaLoanRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, LifeOSDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        authSessionStore = AuthSessionStore(context)
        authSessionStore.saveSession("token", "fuliza-user")
        repository =
            FulizaLoanRepositoryImpl(
                fulizaLoanDao = db.fulizaLoanDao(),
                authSessionStore = authSessionStore,
            )
    }

    @After
    fun tearDown() {
        authSessionStore.clearSession()
        db.close()
    }

    @Test
    fun `repayment with unmatched code is applied to oldest open draws`() =
        runTest {
            repository.recordDraw("DRAW001", amountKes = 300.0, drawDate = 1_700_000_000_000L)
            repository.recordDraw("DRAW002", amountKes = 500.0, drawDate = 1_700_000_001_000L)

            // Repayment SMS code usually differs from draw code.
            repository.recordRepayment(
                drawCode = "REPAY999",
                repaidAmountKes = 400.0,
                repaymentDate = 1_700_000_010_000L,
            )

            val openLoans = repository.observeOpenLoans().first()
            assertEquals(1, openLoans.size)
            assertEquals("DRAW002", openLoans.first().drawCode)
            assertEquals(100.0, openLoans.first().totalRepaidKes, 0.0)
            assertEquals(400.0, repository.observeNetOutstanding().first(), 0.0)
        }
}
