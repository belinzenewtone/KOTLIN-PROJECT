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

/**
 * Tests the three-tier deduplication logic in MpesaDedupeEngineEnhanced:
 *   Tier 1 — M-Pesa code exact match
 *   Tier 2 — SHA-256 hash of raw SMS body
 *   Tier 3 — semantic hash (cross-device dedup)
 */
class MpesaDedupeEngineTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun engineWith(repo: FakeRepo) = MpesaDedupeEngineEnhanced(repo)

    private val merchant = "SAFARICOM"
    private val amount = 250.00
    private val timestamp = 1_718_445_600_000L
    private val rawSms = "SampleSMS body for hashing"
    private val mpesaCode = "TXN123ABC"

    // ── Tier 1 tests ──────────────────────────────────────────────────────────

    @Test
    fun `isDuplicate returns true when mpesa code already stored`() = runTest {
        val repo = FakeRepo(mpesaCodes = setOf(mpesaCode))
        assertTrue(engineWith(repo).isDuplicate(mpesaCode, rawSms, amount, merchant, timestamp))
    }

    @Test
    fun `isDuplicate returns false when nothing stored`() = runTest {
        val repo = FakeRepo()
        assertFalse(engineWith(repo).isDuplicate(mpesaCode, rawSms, amount, merchant, timestamp))
    }

    // ── Tier 2 tests ──────────────────────────────────────────────────────────

    @Test
    fun `isDuplicate returns true when source hash already stored`() = runTest {
        // Pre-compute what the source hash will be, then store it
        val engine = MpesaDedupeEngineEnhanced(FakeRepo())
        val sourceHash = computeSourceHashViaReflection(rawSms)
        val repo = FakeRepo(sourceHashes = setOf(sourceHash))
        assertTrue(engineWith(repo).isDuplicate("DIFFERENT_CODE", rawSms, amount, merchant, timestamp))
    }

    // ── Tier 3 tests ──────────────────────────────────────────────────────────

    @Test
    fun `isDuplicate returns true when semantic hash already stored (cross-device dedup)`() = runTest {
        val engine = MpesaDedupeEngineEnhanced(FakeRepo())
        val semanticHash = engine.computeSemanticHash("TRANSACTION", amount, timestamp, merchant)
        val repo = FakeRepo(semanticHashes = setOf(semanticHash))
        // Different SMS body (different device), same real-world transaction
        assertTrue(
            engineWith(repo).isDuplicate("DIFFERENT_CODE", "Different SMS body", amount, merchant, timestamp),
        )
    }

    @Test
    fun `isDuplicate returns false when semantic hash is different transaction`() = runTest {
        val engine = MpesaDedupeEngineEnhanced(FakeRepo())
        val semanticHashForOtherTx = engine.computeSemanticHash("TRANSACTION", 999.00, timestamp, "OTHER_MERCHANT")
        val repo = FakeRepo(semanticHashes = setOf(semanticHashForOtherTx))
        // Our transaction has different amount/merchant → different semantic hash → NOT a dup
        assertFalse(
            engineWith(repo).isDuplicate("NEW_CODE", "New SMS", amount, merchant, timestamp),
        )
    }
}

// ── Fake repository ───────────────────────────────────────────────────────────

private fun computeSourceHashViaReflection(rawMessage: String): String {
    // Replicate the source-hash logic from MpesaDedupeEngineEnhanced
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(rawMessage.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

internal class FakeRepo(
    private val mpesaCodes: Set<String> = emptySet(),
    private val sourceHashes: Set<String> = emptySet(),
    private val semanticHashes: Set<String> = emptySet(),
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
    override suspend fun existsByMpesaCode(code: String) = code in mpesaCodes
    override suspend fun existsBySourceHash(hash: String) = hash in sourceHashes
    override suspend fun existsBySemanticHash(hash: String) = hash in semanticHashes
    override suspend fun existsPotentialDuplicate(amount: Double, merchant: String, date: Long, windowMillis: Long) = false
    override suspend fun importFromSms(smsBody: String): Transaction? = null
    override suspend fun updateMerchantCategory(merchant: String, category: String) = Unit
    override fun pagedTransactions(startMs: Long?, endMs: Long?, searchQuery: String): Flow<PagingData<Transaction>> =
        flowOf(PagingData.empty())
}
