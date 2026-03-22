package com.personal.lifeOS.platform.sms.dedupe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Verifies that computeSemanticHash() is deterministic and distinguishes
 * transactions with differing fields — a prerequisite for cross-device dedup.
 */
class SemanticHashTest {

    private val engine = MpesaDedupeEngineEnhanced(
        expenseRepository = NoOpExpenseRepository(),
    )

    @Test
    fun `same inputs always produce the same hash`() {
        val hash1 = engine.computeSemanticHash("SENT", 500.00, FIXED_TS, "JOHN DOE")
        val hash2 = engine.computeSemanticHash("SENT", 500.00, FIXED_TS, "JOHN DOE")
        assertEquals("Semantic hash must be deterministic", hash1, hash2)
    }

    @Test
    fun `hash is 64 hex characters (SHA-256)`() {
        val hash = engine.computeSemanticHash("SENT", 500.00, FIXED_TS, "MERCHANT")
        assertEquals(64, hash.length)
        assert(hash.all { it.isLetterOrDigit() }) { "Hash should be hex: $hash" }
    }

    @Test
    fun `different amounts produce different hashes`() {
        val h1 = engine.computeSemanticHash("SENT", 100.00, FIXED_TS, "JOHN")
        val h2 = engine.computeSemanticHash("SENT", 200.00, FIXED_TS, "JOHN")
        assertNotEquals("Different amounts must yield different hashes", h1, h2)
    }

    @Test
    fun `different merchants produce different hashes`() {
        val h1 = engine.computeSemanticHash("SENT", 100.00, FIXED_TS, "ALICE")
        val h2 = engine.computeSemanticHash("SENT", 100.00, FIXED_TS, "BOB")
        assertNotEquals("Different merchants must yield different hashes", h1, h2)
    }

    @Test
    fun `different transaction types produce different hashes`() {
        val h1 = engine.computeSemanticHash("SENT", 100.00, FIXED_TS, "ALICE")
        val h2 = engine.computeSemanticHash("RECEIVED", 100.00, FIXED_TS, "ALICE")
        assertNotEquals("Different types must yield different hashes", h1, h2)
    }

    @Test
    fun `merchant name is normalised (case insensitive)`() {
        val lower = engine.computeSemanticHash("SENT", 100.00, FIXED_TS, "john doe")
        val upper = engine.computeSemanticHash("SENT", 100.00, FIXED_TS, "JOHN DOE")
        assertEquals("Merchant comparison must be case-insensitive", lower, upper)
    }

    @Test
    fun `merchant name is normalised (leading-trailing whitespace)`() {
        val clean = engine.computeSemanticHash("SENT", 100.00, FIXED_TS, "JOHN DOE")
        val padded = engine.computeSemanticHash("SENT", 100.00, FIXED_TS, "  JOHN DOE  ")
        assertEquals("Merchant comparison must trim whitespace", clean, padded)
    }

    @Test
    fun `timestamps on the same calendar day yield the same hash`() {
        // Noon vs evening on the same day → date portion is identical
        val noonTs = FIXED_TS
        val eveningTs = FIXED_TS + 6 * 3_600_000L // +6 hours, still same calendar date
        val h1 = engine.computeSemanticHash("SENT", 100.00, noonTs, "MERCHANT")
        val h2 = engine.computeSemanticHash("SENT", 100.00, eveningTs, "MERCHANT")
        assertEquals("Same calendar day must produce same hash", h1, h2)
    }

    @Test
    fun `timestamps on different calendar days yield different hashes`() {
        val day1 = FIXED_TS
        val day2 = FIXED_TS + 25 * 3_600_000L // +25 hours = next day
        val h1 = engine.computeSemanticHash("SENT", 100.00, day1, "MERCHANT")
        val h2 = engine.computeSemanticHash("SENT", 100.00, day2, "MERCHANT")
        assertNotEquals("Different calendar days must produce different hashes", h1, h2)
    }

    private companion object {
        // 2024-06-15 12:00:00 UTC
        const val FIXED_TS = 1_718_445_600_000L
    }
}
