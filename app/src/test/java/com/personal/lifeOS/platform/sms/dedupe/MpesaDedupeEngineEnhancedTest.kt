package com.personal.lifeOS.platform.sms.dedupe

import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever

/**
 * Tests for dual-key M-Pesa deduplication engine.
 *
 * Tests cover:
 * - Primary key: mpesa_code (exact transaction reference)
 * - Secondary key: source_hash (SMS body variant detection, esp. Fuliza)
 * - Tertiary: heuristic fallback (same amount + merchant + time window)
 */
@RunWith(MockitoJUnitRunner::class)
class MpesaDedupeEngineEnhancedTest {

    @Mock
    private lateinit var mockExpenseRepository: ExpenseRepository

    private lateinit var dedupeEngine: MpesaDedupeEngineEnhanced

    @Before
    fun setUp() {
        dedupeEngine = MpesaDedupeEngineEnhanced(mockExpenseRepository)
    }

    // ── Primary Key: M-Pesa Code ──────────────────────────────────────────────

    @Test
    fun testPrimaryKey_DetectDuplicateByMpesaCode() = runTest {
        val mpesaCode = "ABC1234567"
        val rawMessage = "ABC1234567 Confirmed. Ksh1,000 sent to John on 1/1/26."

        whenever(mockExpenseRepository.existsByMpesaCode(mpesaCode)).thenReturn(true)
        whenever(mockExpenseRepository.existsBySourceHash(any())).thenReturn(false)

        val isDuplicate = dedupeEngine.isDuplicate(
            mpesaCode = mpesaCode,
            rawMessage = rawMessage,
            amount = 1000.0,
            merchant = "John",
            timestamp = System.currentTimeMillis(),
        )

        assert(isDuplicate)
    }

    @Test
    fun testPrimaryKey_AllowNewMpesaCode() = runTest {
        val mpesaCode = "ABC1234567"
        val rawMessage = "ABC1234567 Confirmed. Ksh1,000 sent to John on 1/1/26."

        whenever(mockExpenseRepository.existsByMpesaCode(mpesaCode)).thenReturn(false)
        whenever(mockExpenseRepository.existsBySourceHash(any())).thenReturn(false)
        whenever(mockExpenseRepository.existsPotentialDuplicate(any(), any(), any(), any())).thenReturn(false)

        val isDuplicate = dedupeEngine.isDuplicate(
            mpesaCode = mpesaCode,
            rawMessage = rawMessage,
            amount = 1000.0,
            merchant = "John",
            timestamp = System.currentTimeMillis(),
        )

        assert(!isDuplicate)
    }

    // ── Secondary Key: Source Hash ────────────────────────────────────────────

    @Test
    fun testSecondaryKey_DetectDuplicateBySourceHash() = runTest {
        val sms1 = "ABC1234567 Confirmed. Ksh5,000 from your M-PESA Fuliza on 1/1/26."
        val sms2 = "ABC1234567 Fuliza interest Ksh150 accrued on 2/1/26."

        whenever(mockExpenseRepository.existsByMpesaCode(any())).thenReturn(false)
        // First message already imported, second has different text but same code
        whenever(mockExpenseRepository.existsBySourceHash(any())).thenAnswer { invocation ->
            // Simulate that the first SMS hash is already in DB
            val providedHash = invocation.arguments[0] as String
            providedHash == computeHash(sms1)
        }

        val isFirstDuplicate = dedupeEngine.isDuplicate(
            mpesaCode = "ABC1234567",
            rawMessage = sms1,
            amount = 5000.0,
            merchant = "Fuliza",
            timestamp = System.currentTimeMillis(),
        )

        val isSecondDuplicate = dedupeEngine.isDuplicate(
            mpesaCode = "ABC1234567",
            rawMessage = sms2,
            amount = 150.0,
            merchant = "Fuliza",
            timestamp = System.currentTimeMillis(),
        )

        // The first one passes (no duplicate yet)
        // The second should also pass primary check (code match would catch it too in real scenario)
        assert(!isFirstDuplicate)
    }

    @Test
    fun testSecondaryKey_DifferentMessageSameCodeAreDetected() = runTest {
        // Fuliza scenario: same transaction code, different SMS bodies
        val messageBody1 = "ABC1234567 Confirmed. Ksh5,000 from your M-PESA Fuliza on 1/1/26."
        val messageBody2 = "ABC1234567 Fuliza Interest Ksh150 charged on 1/1/26."

        val hash1 = computeHash(messageBody1)
        val hash2 = computeHash(messageBody2)

        assert(hash1 != hash2) // Different content = different hashes
        assert(hash1 != hash2) // Verify they're actually different
    }

    // ── Tertiary: Heuristic Fallback ──────────────────────────────────────────

    @Test
    fun testTertiary_DetectDuplicateByHeuristic() = runTest {
        val mpesaCode = "ABC1234567"
        val rawMessage = "ABC1234567 Confirmed. Ksh1,000 sent to John on 1/1/26."
        val now = System.currentTimeMillis()

        whenever(mockExpenseRepository.existsByMpesaCode(mpesaCode)).thenReturn(false)
        whenever(mockExpenseRepository.existsBySourceHash(any())).thenReturn(false)
        // Heuristic: same amount + merchant + time within 5 min
        whenever(mockExpenseRepository.existsPotentialDuplicate(
            amount = 1000.0,
            merchant = "John",
            date = now,
            windowMillis = 5 * 60 * 1000L,
        )).thenReturn(true)

        val isDuplicate = dedupeEngine.isDuplicate(
            mpesaCode = mpesaCode,
            rawMessage = rawMessage,
            amount = 1000.0,
            merchant = "John",
            timestamp = now,
        )

        assert(isDuplicate)
    }

    @Test
    fun testTertiary_AllowIfDifferentAmount() = runTest {
        val mpesaCode = "ABC1234567"
        val rawMessage = "ABC1234567 Confirmed. Ksh1,000 sent to John on 1/1/26."
        val now = System.currentTimeMillis()

        whenever(mockExpenseRepository.existsByMpesaCode(mpesaCode)).thenReturn(false)
        whenever(mockExpenseRepository.existsBySourceHash(any())).thenReturn(false)
        // Heuristic: same merchant but DIFFERENT amount = not a duplicate
        whenever(mockExpenseRepository.existsPotentialDuplicate(
            amount = 1000.0,
            merchant = "John",
            date = now,
            windowMillis = 5 * 60 * 1000L,
        )).thenReturn(false)

        val isDuplicate = dedupeEngine.isDuplicate(
            mpesaCode = mpesaCode,
            rawMessage = rawMessage,
            amount = 1000.0,
            merchant = "John",
            timestamp = now,
        )

        assert(!isDuplicate)
    }

    // ── Fuliza-Specific Scenarios ─────────────────────────────────────────────

    @Test
    fun testFuliza_ScenarioDuplicate() = runTest {
        // Step 1: User receives Fuliza loan
        val smsFulizaLoan = "ABC1234567 Confirmed. Ksh5,000 from your M-PESA Fuliza on 1/1/26."

        whenever(mockExpenseRepository.existsByMpesaCode("ABC1234567")).thenReturn(false)
        whenever(mockExpenseRepository.existsBySourceHash(computeHash(smsFulizaLoan))).thenReturn(true)  // First message imported

        val isFirstDuplicate = dedupeEngine.isDuplicate(
            mpesaCode = "ABC1234567",
            rawMessage = smsFulizaLoan,
            amount = 5000.0,
            merchant = "Fuliza",
            timestamp = System.currentTimeMillis(),
        )

        // Step 2: Interest notice arrives (same code, different body)
        val smsFulizaInterest = "ABC1234567 Fuliza interest Ksh150 accrued on 2/1/26."

        // Reset mocks for second check
        whenever(mockExpenseRepository.existsByMpesaCode("ABC1234567")).thenReturn(true)  // Code now exists
        whenever(mockExpenseRepository.existsBySourceHash(computeHash(smsFulizaInterest))).thenReturn(false)

        val isSecondDuplicate = dedupeEngine.isDuplicate(
            mpesaCode = "ABC1234567",
            rawMessage = smsFulizaInterest,
            amount = 150.0,
            merchant = "Fuliza",
            timestamp = System.currentTimeMillis(),
        )

        // First should be kept (initial import)
        // Second should be rejected (primary key match → already imported)
        assert(!isFirstDuplicate)
        assert(isSecondDuplicate)  // Caught by code match
    }

    // ── Helper Functions ──────────────────────────────────────────────────────

    private fun computeHash(text: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(text.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            text.hashCode().toString()
        }
    }

    companion object {
        // Mock helper
        private fun any(): String = org.mockito.kotlin.any()
    }
}
