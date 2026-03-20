package com.personal.lifeOS.platform.sms.parser

import com.personal.lifeOS.platform.sms.parser.MpesaParsingConfig.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive tests for the enhanced M-Pesa parser with 6-stage pipeline.
 *
 * Tests cover:
 * 1. Stage 1: M-Pesa code extraction
 * 2. Stage 2: Amount extraction
 * 3. Stage 3: Transaction intent classification (detection rules)
 * 4. Stage 4: Counterparty extraction
 * 5. Stage 5: Confidence scoring
 * 6. Stage 6: Description generation
 * + Edge cases: Fuliza duplicates, reversals, regional variants
 */
class MpesaParserEnhancedTest {

    // ── Stage 1: Code Extraction ──────────────────────────────────────────────

    @Test
    fun testStage1_ExtractMpesaCodeFromStart() {
        val sms = "ABC1234567 Confirmed. Ksh100 sent to John on 1/1/26 at 10:00 AM."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertNotNull(parsed)
        assertEquals("ABC1234567", parsed?.mpesaCode)
    }

    @Test
    fun testStage1_RejectIfNoCod() {
        val sms = "Ksh100 sent to John on 1/1/26 at 10:00 AM. No M-Pesa code!"
        val parsed = MpesaParserEnhanced.parse(sms)
        assertNull(parsed)  // No code → not M-Pesa
    }

    // ── Stage 2: Amount Extraction ────────────────────────────────────────────

    @Test
    fun testStage2_ExtractAmountKsh() {
        val sms = "ABC1234567 Confirmed. Ksh1,250.50 sent to John on 1/1/26 at 10:00 AM."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertEquals(1250.50, parsed?.amount, 0.01)
    }

    @Test
    fun testStage2_ExtractAmountKES() {
        val sms = "ABC1234567 Confirmed. KES 999 sent to John on 1/1/26 at 10:00 AM."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertEquals(999.0, parsed?.amount, 0.01)
    }

    @Test
    fun testStage2_RejectIfNoAmount() {
        val sms = "ABC1234567 Confirmed. sent to John on 1/1/26 at 10:00 AM."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertNull(parsed)  // No amount → invalid
    }

    @Test
    fun testStage2_RejectZeroOrNegativeAmount() {
        val sms = "ABC1234567 Confirmed. Ksh0 sent to John on 1/1/26 at 10:00 AM."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertNull(parsed)  // Zero amount → invalid
    }

    // ── Stage 3: Transaction Intent Classification ────────────────────────────

    @Test
    fun testStage3_ClassifyReversal() {
        val sms = "ABC1234567 Confirmed. Ksh500 sent to John has been reversed on 1/1/26."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertEquals(TransactionCategory.REVERSED, parsed?.category)
        assertEquals(Confidence.HIGH, parsed?.confidence)
    }

    @Test
    fun testStage3_ClassifyReceived() {
        val sms = "ABC1234567 Confirmed. You have received Ksh1,000 from JANE DOE on 1/1/26 at 10:00 AM."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertEquals(TransactionCategory.RECEIVED, parsed?.category)
        assertEquals(Confidence.HIGH, parsed?.confidence)
    }

    @Test
    fun testStage3_ClassifyAirtime() {
        val sms = "ABC1234567 Confirmed. Ksh100 of airtime purchased on 1/1/26 at 10:00 AM."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertEquals(TransactionCategory.AIRTIME, parsed?.category)
        assertEquals(Confidence.HIGH, parsed?.confidence)
    }

    @Test
    fun testStage3_ClassifyPaybill() {
        val sms = "ABC1234567 Confirmed. Ksh1,250 sent to KPLC PREPAID for account 998877 on 1/1/26."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertEquals(TransactionCategory.PAYBILL, parsed?.category)
        assertEquals(Confidence.HIGH, parsed?.confidence)
    }

    @Test
    fun testStage3_ClassifyBuyGoods() {
        val sms = "ABC1234567 Confirmed. Ksh500 paid to NAIVAS WESTLANDS on 1/1/26 at 10:00 AM."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertEquals(TransactionCategory.BUY_GOODS, parsed?.category)
        assertEquals(Confidence.HIGH, parsed?.confidence)
    }

    @Test
    fun testStage3_ClassifyWithdrawal() {
        val sms = "ABC1234567 Confirmed. Ksh5,000 withdrawn from Agent 123456 on 1/1/26 at 10:00 AM."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertEquals(TransactionCategory.WITHDRAW, parsed?.category)
        assertEquals(Confidence.HIGH, parsed?.confidence)
    }

    @Test
    fun testStage3_ClassifySent() {
        val sms = "ABC1234567 Confirmed. Ksh2,000 sent to JOHN DOE on 1/1/26 at 10:00 AM."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertEquals(TransactionCategory.SENT, parsed?.category)
        assertEquals(Confidence.HIGH, parsed?.confidence)
    }

    @Test
    fun testStage3_RuleOrderingMatters_ReversalBeforeReceived() {
        // This SMS would match BOTH reversal AND received if rules were unordered
        val sms = "ABC1234567 Confirmed. Ksh500 received from John has been reversed on 1/1/26."
        val parsed = MpesaParserEnhanced.parse(sms)
        // Should be classified as REVERSAL (more specific), not RECEIVED
        assertEquals(TransactionCategory.REVERSED, parsed?.category)
    }

    // ── Stage 4: Counterparty Extraction ──────────────────────────────────────

    @Test
    fun testStage4_ExtractCounterpartyFromReceived() {
        val sms = "ABC1234567 Confirmed. You have received Ksh1,000 from JANE DOE on 1/1/26."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertEquals("JANE DOE", parsed?.counterparty)
    }

    @Test
    fun testStage4_ExtractCounterpartyFromPaybill() {
        val sms = "ABC1234567 Confirmed. Ksh1,250 sent to KPLC PREPAID for account 998877 on 1/1/26."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertEquals("KPLC PREPAID", parsed?.counterparty)
    }

    @Test
    fun testStage4_DefaultCounterpartyForAirtime() {
        val sms = "ABC1234567 Confirmed. Ksh100 of airtime purchased on 1/1/26."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertEquals("Airtime Purchase", parsed?.counterparty)
    }

    // ── Stage 5: Confidence Scoring ───────────────────────────────────────────

    @Test
    fun testStage5_HighConfidenceForStructuralPattern() {
        val sms = "ABC1234567 Confirmed. Ksh500 sent to John on 1/1/26 at 10:00 AM."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertEquals(Confidence.HIGH, parsed?.confidence)
    }

    @Test
    fun testStage5_MediumConfidenceForFuliza() {
        val sms = "ABC1234567 Confirmed. Ksh1,000 from your M-PESA Fuliza on 1/1/26."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertEquals(Confidence.MEDIUM, parsed?.confidence)
    }

    // ── Stage 6: Description Generation ───────────────────────────────────────

    @Test
    fun testStage6_GenerateDescriptionWithCounterparty() {
        val sms = "ABC1234567 Confirmed. You have received Ksh1,000 from JANE DOE on 1/1/26."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertEquals("Received from JANE DOE", parsed?.description)
    }

    @Test
    fun testStage6_GenerateDescriptionFallback() {
        val sms = "ABC1234567 Confirmed. Ksh100 of airtime purchased on 1/1/26."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertTrue(parsed?.description?.contains("Airtime") ?: false)
    }

    // ── Edge Cases: Fuliza Handling ───────────────────────────────────────────

    @Test
    fun testFulizaServiceNoticeFiltering() {
        // Fuliza service notice should be filtered out
        val sms = "Fuliza service charge Ksh150 deducted. Interest accrual on ABC1234567 on 1/1/26."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertNull(parsed)  // Should be ignored
    }

    @Test
    fun testFulizaTransactionVsFulizaNotice() {
        // Actual Fuliza loan transaction (should be parsed)
        val smsFulizaTx = "ABC1234567 Confirmed. Ksh5,000 from your M-PESA Fuliza on 1/1/26."
        val parsed = MpesaParserEnhanced.parse(smsFulizaTx)
        assertNotNull(parsed)
        assertEquals(TransactionCategory.LOAN, parsed?.category)
    }

    @Test
    fun testFulizaDuplicateScenario_BothMessagesShouldParse() {
        // Original Fuliza loan
        val smsFulizaLoan = "ABC1234567 Confirmed. Ksh5,000 from your M-PESA Fuliza on 1/1/26."
        val loan = MpesaParserEnhanced.parse(smsFulizaLoan)
        assertNotNull(loan)
        assertEquals("ABC1234567", loan?.mpesaCode)
        assertEquals(5000.0, loan?.amount, 0.01)

        // Fuliza interest notice (different amount, same code)
        val smsFulizaInterest = "ABC1234567 Fuliza interest Ksh150 accrued on 2/1/26."
        val interest = MpesaParserEnhanced.parse(smsFulizaInterest)
        // This would parse as LOAN but deduplication should catch the code match
        assertNotNull(interest)  // Parser succeeds, dedup engine catches duplicate
    }

    // ── Edge Cases: Regional/Legacy Variants ──────────────────────────────────

    @Test
    fun testRegionalVariant_Deposit() {
        val sms = "ABC1234567 Confirmed. Ksh2,500 deposited into your M-Pesa on 1/1/26."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertNotNull(parsed)
        // Deposits are categorized as DEPOSIT (distinct from P2P received)
        assertEquals(TransactionCategory.DEPOSIT, parsed?.category)
    }

    @Test
    fun testRegionalVariant_CaseInsensitivity() {
        val sms = "ABC1234567 Confirmed. ksh500 SENT TO JOHN on 1/1/26 at 10:00 AM."
        val parsed = MpesaParserEnhanced.parse(sms)
        assertNotNull(parsed)
        assertEquals(500.0, parsed?.amount, 0.01)
    }

    // ── Stage 0: Fast Filter ──────────────────────────────────────────────────

    @Test
    fun testStage0_QuickFilterRejectNonMpesaSms() {
        val nonMpesaSms = "Hey John, can you help me with the project?"
        assertFalse(MpesaParserEnhanced.isMpesaSms(nonMpesaSms))
    }

    @Test
    fun testStage0_QuickFilterAcceptMpesaSms() {
        val mpesaSms = "ABC1234567 Confirmed. Ksh500 sent to John on 1/1/26 at 10:00 AM."
        assertTrue(MpesaParserEnhanced.isMpesaSms(mpesaSms))
    }

    // ── Performance Tests ─────────────────────────────────────────────────────

    @Test
    fun testPerformance_ParsingTakesLessThan5ms() {
        val sms = "ABC1234567 Confirmed. Ksh1,250 sent to JOHN DOE on 7/3/26 at 10:15 AM."
        val startTime = System.nanoTime()
        val parsed = MpesaParserEnhanced.parse(sms)
        val endTime = System.nanoTime()
        val durationMs = (endTime - startTime) / 1_000_000.0

        assertNotNull(parsed)
        assertTrue("Parsing took ${durationMs}ms, should be < 5ms", durationMs < 5.0)
    }

    @Test
    fun testPerformance_ParseMultipleMessages() {
        val messages = (1..10).map {
            "ABC123456$it Confirmed. Ksh${1000 + it} sent to PERSON $it on 1/1/26 at 10:00 AM."
        }

        val startTime = System.nanoTime()
        val results = messages.map { MpesaParserEnhanced.parse(it) }
        val endTime = System.nanoTime()
        val durationMs = (endTime - startTime) / 1_000_000.0

        assertEquals(10, results.size)
        assertTrue("Parsing 10 messages took ${durationMs}ms, should be < 50ms", durationMs < 50.0)
    }
}
