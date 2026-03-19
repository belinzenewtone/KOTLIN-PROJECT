package com.personal.lifeOS.platform.sms.parser

import com.personal.lifeOS.features.expenses.data.parser.MpesaSmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class MpesaMessageParserFixturesTest {
    @Test
    fun `parser handles canonical fixture variants`() {
        MpesaParserFixtures.entries.forEach { fixture ->
            val parsed = MpesaSmsParser.parse(fixture.sms)
            assertNotNull("Expected fixture ${fixture.name} to parse", parsed)
            assertEquals(fixture.code, parsed?.mpesaCode)
            assertEquals(fixture.amount, parsed?.amount ?: 0.0, 0.001)
            assertEquals(fixture.merchant, parsed?.merchant)
            assertEquals(fixture.type, parsed?.transactionType)
            assertTrue((parsed?.date ?: 0L) > 0L)
        }
    }

    @Test
    fun `parser supports four-digit year timestamps`() {
        val sms =
            "QY11223344 Confirmed. Ksh1,100.00 sent to MARY ANN 0711002200 on 19/3/2026 at 9:30 AM."

        val parsed = MpesaSmsParser.parse(sms)
        assertNotNull(parsed)

        val calendar =
            Calendar.getInstance().apply {
                timeInMillis = parsed?.date ?: 0L
            }
        assertEquals(2026, calendar.get(Calendar.YEAR))
    }

    @Test
    fun `parser preserves unknown type when message has no known verb`() {
        val sms =
            "QZ66778899 Confirmed. Ksh120.00 transaction reference only on 19/3/26 at 8:00 AM."

        val parsed = MpesaSmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(MpesaSmsParser.TransactionType.UNKNOWN, parsed?.transactionType)
        assertEquals("Unknown", parsed?.merchant)
    }
}
