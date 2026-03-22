package com.personal.lifeOS.platform.sms.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Verifies that MpesaParserEnhanced is thread-safe when called concurrently from
 * multiple coroutines — a critical guarantee for WorkManager's parallel import tasks.
 *
 * The parser previously used SimpleDateFormat (thread-unsafe) and was replaced with
 * the immutable DateTimeFormatter. These tests guard against regression.
 */
class MpesaParserThreadSafetyTest {

    // A real Safaricom "sent" SMS template
    private val sentSms =
        "PBK90XTEST Confirmed. Ksh500.00 sent to JOHN DOE 0712345678 on 15/6/24 at 10:30 AM. " +
            "New M-PESA balance is Ksh1,234.00. Transaction cost, Ksh0.00."

    // A real Safaricom "paybill" SMS template
    private val paybillSms =
        "SLK12ZTEST Confirmed. Ksh2,500.00 paid to KPLC PREPAID for account 12345678 on 15/6/24 " +
            "at 2:15 PM. New M-PESA balance is Ksh8,500.00. Transaction cost, Ksh35.00."

    @Test
    fun `parser returns non-null for valid sent SMS`() {
        val result = MpesaParserEnhanced.parse(sentSms)
        assertNotNull("Valid sent SMS must parse successfully", result)
    }

    @Test
    fun `concurrent parses of same SMS produce identical results`() = runTest {
        val concurrency = 50
        val results =
            (1..concurrency)
                .map {
                    async(Dispatchers.Default) {
                        MpesaParserEnhanced.parse(sentSms)
                    }
                }
                .awaitAll()

        // All results must be non-null
        results.forEach { result ->
            assertNotNull("All concurrent parses must succeed", result)
        }

        // All dates must be identical — this fails if DateTimeFormatter usage is not thread-safe
        val dates = results.mapNotNull { it?.date }
        val distinctDates = dates.distinct()
        assertEquals(
            "All concurrent parses must return the same parsed date. " +
                "Got distinct values: $distinctDates",
            1,
            distinctDates.size,
        )
    }

    @Test
    fun `concurrent parses of different SMS types do not corrupt each other`() = runTest {
        val results =
            (1..40)
                .map { i ->
                    async(Dispatchers.Default) {
                        if (i % 2 == 0) {
                            MpesaParserEnhanced.parse(sentSms)
                        } else {
                            MpesaParserEnhanced.parse(paybillSms)
                        }
                    }
                }
                .awaitAll()

        // No result should be null — parsers must not corrupt each other's state
        results.forEachIndexed { index, result ->
            assertNotNull("Parse at index $index must not be null", result)
        }

        // All parsed amounts for paybill SMS must be 2500.0
        val paybillResults = results.filterIndexed { index, _ -> index % 2 == 1 }
        paybillResults.forEach { result ->
            assertEquals(
                "Paybill SMS amount must not be corrupted by concurrent sent-SMS parse",
                2500.0,
                result!!.amount,
                0.01,
            )
        }
    }

    @Test
    fun `isMpesaSms correctly identifies real SMS`() {
        assert(MpesaParserEnhanced.isMpesaSms(sentSms)) { "Sent SMS must be recognised as M-Pesa" }
        assert(MpesaParserEnhanced.isMpesaSms(paybillSms)) { "Paybill SMS must be recognised as M-Pesa" }
        assert(!MpesaParserEnhanced.isMpesaSms("Hello, are you free tonight?")) {
            "Random text must not be recognised as M-Pesa"
        }
    }
}
