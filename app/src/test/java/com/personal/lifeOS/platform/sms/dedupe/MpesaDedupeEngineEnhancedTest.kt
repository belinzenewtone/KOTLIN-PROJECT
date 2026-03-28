package com.personal.lifeOS.platform.sms.dedupe

import androidx.paging.PagingData
import com.personal.lifeOS.features.expenses.domain.model.CategoryBreakdown
import com.personal.lifeOS.features.expenses.domain.model.Transaction
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MpesaDedupeEngineEnhancedTest {
    @Test
    fun `duplicate is detected from exact mpesa code first`() = runTest {
        val repository = FakeExpenseRepository(existingMpesaCodes = setOf("ABC1234567"))
        val engine = MpesaDedupeEngineEnhanced(repository)

        val duplicate = engine.isDuplicate(
            mpesaCode = "ABC1234567",
            rawMessage = "ABC1234567 Confirmed. Ksh1,000 sent to John on 1/1/26.",
            amount = 1000.0,
            merchant = "John",
            timestamp = 1_700_000_000_000L,
        )

        assertTrue(duplicate)
    }

    @Test
    fun `duplicate is detected from source hash when code is new`() = runTest {
        val repository = FakeExpenseRepository(existingSourceHashes = setOf(hashOf("seed message")))
        val engine = MpesaDedupeEngineEnhanced(repository)
        val rawMessage = "seed message"

        val duplicate = engine.isDuplicate(
            mpesaCode = "NEW1234567",
            rawMessage = rawMessage,
            amount = 1000.0,
            merchant = "John",
            timestamp = 1_700_000_000_000L,
        )

        assertTrue(duplicate)
    }

    @Test
    fun `duplicate is detected from semantic hash across device variants`() = runTest {
        val repository = FakeExpenseRepository(
            existingSemanticHashes = setOf(
                semanticHashFor(
                    transactionType = "TRANSACTION",
                    amount = 2500.0,
                    timestampMs = 1_700_000_000_000L,
                    counterparty = "KPLC PREPAID",
                ),
            ),
        )
        val engine = MpesaDedupeEngineEnhanced(repository)

        val duplicate = engine.isDuplicate(
            mpesaCode = "XYZ1234567",
            rawMessage = "different body entirely",
            amount = 2500.0,
            merchant = "KPLC PREPAID",
            timestamp = 1_700_000_000_000L,
        )

        assertTrue(duplicate)
    }

    @Test
    fun `heuristic duplicate catches same amount merchant and time window`() = runTest {
        val repository = FakeExpenseRepository(heuristicDuplicate = true)
        val engine = MpesaDedupeEngineEnhanced(repository)

        val duplicate = engine.isDuplicate(
            mpesaCode = "UNIQUE1234",
            rawMessage = "UNIQUE1234 Confirmed. Ksh1,000 sent to John on 1/1/26.",
            amount = 1000.0,
            merchant = "John",
            timestamp = 1_700_000_000_000L,
        )

        assertTrue(duplicate)
    }

    @Test
    fun `new transaction passes when no dedupe key matches`() = runTest {
        val repository = FakeExpenseRepository()
        val engine = MpesaDedupeEngineEnhanced(repository)

        val duplicate = engine.isDuplicate(
            mpesaCode = "UNIQUE1234",
            rawMessage = "UNIQUE1234 Confirmed. Ksh1,000 sent to John on 1/1/26.",
            amount = 1000.0,
            merchant = "John",
            timestamp = 1_700_000_000_000L,
        )

        assertFalse(duplicate)
    }

    private fun hashOf(text: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun semanticHashFor(
        transactionType: String,
        amount: Double,
        timestampMs: Long,
        counterparty: String,
    ): String {
        return MpesaDedupeEngineEnhanced(FakeExpenseRepository()).computeSemanticHash(
            transactionType = transactionType,
            amount = amount,
            timestampMs = timestampMs,
            counterparty = counterparty,
        )
    }
}

private class FakeExpenseRepository(
    private val existingMpesaCodes: Set<String> = emptySet(),
    private val existingSourceHashes: Set<String> = emptySet(),
    private val existingSemanticHashes: Set<String> = emptySet(),
    private val heuristicDuplicate: Boolean = false,
) : ExpenseRepository {
    override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(emptyList())

    override fun getTransactionsBetween(start: Long, end: Long): Flow<List<Transaction>> = flowOf(emptyList())

    override fun getByCategory(category: String): Flow<List<Transaction>> = flowOf(emptyList())

    override fun getTotalSpendingBetween(start: Long, end: Long): Flow<Double> = flowOf(0.0)

    override fun getCategoryBreakdown(start: Long, end: Long): Flow<List<CategoryBreakdown>> = flowOf(emptyList())

    override fun getTransactionCount(): Flow<Int> = flowOf(0)

    override suspend fun addTransaction(transaction: Transaction): Long = 0L

    override suspend fun updateTransaction(transaction: Transaction) = Unit

    override suspend fun deleteTransaction(transaction: Transaction) = Unit

    override suspend fun getById(id: Long): Transaction? = null

    override suspend fun existsByMpesaCode(code: String): Boolean = code in existingMpesaCodes

    override suspend fun existsBySourceHash(sourceHash: String): Boolean = sourceHash in existingSourceHashes

    override suspend fun existsBySemanticHash(semanticHash: String): Boolean = semanticHash in existingSemanticHashes

    override suspend fun existsPotentialDuplicate(
        amount: Double,
        merchant: String,
        date: Long,
        windowMillis: Long,
    ): Boolean = heuristicDuplicate

    override suspend fun importFromSms(smsBody: String): Transaction? = null

    override suspend fun updateMerchantCategory(merchant: String, category: String) = Unit

    override fun pagedTransactions(
        startMs: Long?,
        endMs: Long?,
        searchQuery: String,
    ): Flow<PagingData<Transaction>> = flowOf(PagingData.empty())
}
