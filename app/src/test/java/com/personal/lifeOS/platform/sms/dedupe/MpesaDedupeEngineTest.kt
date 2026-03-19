package com.personal.lifeOS.platform.sms.dedupe

import com.personal.lifeOS.features.expenses.domain.model.CategoryBreakdown
import com.personal.lifeOS.features.expenses.domain.model.Transaction
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MpesaDedupeEngineTest {
    @Test
    fun `returns true when mpesa code already exists`() =
        runTest {
            val repository =
                FakeExpenseRepository(
                    duplicateByCode = true,
                    duplicateByHeuristic = false,
                )
            val engine = MpesaDedupeEngine(repository)

            val result =
                engine.isDuplicate(
                    mpesaCode = "QH11112222",
                    amount = 1200.0,
                    merchant = "CARREFOUR",
                    timestamp = 1700000000000L,
                )

            assertTrue(result)
            assertEquals(0, repository.heuristicChecks)
        }

    @Test
    fun `returns true when heuristic duplicate exists`() =
        runTest {
            val repository =
                FakeExpenseRepository(
                    duplicateByCode = false,
                    duplicateByHeuristic = true,
                )
            val engine = MpesaDedupeEngine(repository)

            val result =
                engine.isDuplicate(
                    mpesaCode = "QH33334444",
                    amount = 500.0,
                    merchant = "NAIVAS",
                    timestamp = 1700000000000L,
                )

            assertTrue(result)
            assertEquals(1, repository.heuristicChecks)
        }

    @Test
    fun `returns false when no duplicate signal exists`() =
        runTest {
            val repository =
                FakeExpenseRepository(
                    duplicateByCode = false,
                    duplicateByHeuristic = false,
                )
            val engine = MpesaDedupeEngine(repository)

            val result =
                engine.isDuplicate(
                    mpesaCode = "QH55556666",
                    amount = 250.0,
                    merchant = "KIOSK",
                    timestamp = 1700000000000L,
                )

            assertFalse(result)
            assertEquals(1, repository.heuristicChecks)
        }
}

private class FakeExpenseRepository(
    private val duplicateByCode: Boolean,
    private val duplicateByHeuristic: Boolean,
) : ExpenseRepository {
    var heuristicChecks: Int = 0

    override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(emptyList())

    override fun getTransactionsBetween(
        start: Long,
        end: Long,
    ): Flow<List<Transaction>> = flowOf(emptyList())

    override fun getByCategory(category: String): Flow<List<Transaction>> = flowOf(emptyList())

    override fun getTotalSpendingBetween(
        start: Long,
        end: Long,
    ): Flow<Double> = flowOf(0.0)

    override fun getCategoryBreakdown(
        start: Long,
        end: Long,
    ): Flow<List<CategoryBreakdown>> = flowOf(emptyList())

    override fun getTransactionCount(): Flow<Int> = flowOf(0)

    override suspend fun addTransaction(transaction: Transaction): Long = 0L

    override suspend fun updateTransaction(transaction: Transaction) = Unit

    override suspend fun deleteTransaction(transaction: Transaction) = Unit

    override suspend fun getById(id: Long): Transaction? = null

    override suspend fun existsByMpesaCode(code: String): Boolean = duplicateByCode

    override suspend fun existsPotentialDuplicate(
        amount: Double,
        merchant: String,
        date: Long,
        windowMillis: Long,
    ): Boolean {
        heuristicChecks += 1
        return duplicateByHeuristic
    }

    override suspend fun importFromSms(smsBody: String): Transaction? = null

    override suspend fun updateMerchantCategory(
        merchant: String,
        category: String,
    ) = Unit
}
