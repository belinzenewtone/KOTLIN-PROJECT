package com.personal.lifeOS.features.expenses.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MpesaSmsParserTest {
    @Test
    fun `isMpesaSms returns true for valid mpesa content`() {
        val sms =
            "QH12345678 Confirmed. Ksh1,250.00 sent to JOHN DOE 0712345678 on 7/3/26 at 10:15 AM."

        assertTrue(MpesaSmsParser.isMpesaSms(sms))
    }

    @Test
    fun `parse extracts sent transaction fields`() {
        val sms =
            "QH12345678 Confirmed. Ksh1,250.00 sent to JOHN DOE 0712345678 on 7/3/26 at 10:15 AM. New M-PESA balance is Ksh300.00."

        val parsed = MpesaSmsParser.parse(sms)

        assertNotNull(parsed)
        assertEquals("QH12345678", parsed?.mpesaCode)
        assertEquals(1250.0, parsed?.amount ?: 0.0, 0.001)
        assertEquals(MpesaSmsParser.TransactionType.SENT, parsed?.transactionType)
        assertEquals("JOHN DOE", parsed?.merchant)
        assertTrue((parsed?.date ?: 0L) > 0L)
    }

    @Test
    fun `parse returns null for non mpesa content`() {
        val sms = "Utility reminder only. Your monthly statement is ready."
        assertNull(MpesaSmsParser.parse(sms))
    }

    @Test
    fun `parse extracts received transaction variant`() {
        val sms =
            "QJ22334455 Confirmed. You have received Ksh2,300.00 from JANE DOE 0712000000 on 14/3/26 at 8:45 AM."

        val parsed = MpesaSmsParser.parse(sms)

        assertNotNull(parsed)
        assertEquals("QJ22334455", parsed?.mpesaCode)
        assertEquals(2300.0, parsed?.amount ?: 0.0, 0.001)
        assertEquals(MpesaSmsParser.TransactionType.RECEIVED, parsed?.transactionType)
        assertEquals("JANE DOE", parsed?.merchant)
    }

    @Test
    fun `parse extracts paid transaction variant`() {
        val sms =
            "QK99887766 Confirmed. Ksh540.00 paid to NAIVAS WESTLANDS on 18/3/26 at 7:20 PM. " +
                "New M-PESA balance is Ksh1,200.00."

        val parsed = MpesaSmsParser.parse(sms)

        assertNotNull(parsed)
        assertEquals("QK99887766", parsed?.mpesaCode)
        assertEquals(540.0, parsed?.amount ?: 0.0, 0.001)
        assertEquals(MpesaSmsParser.TransactionType.PAID, parsed?.transactionType)
        assertEquals("NAIVAS WESTLANDS", parsed?.merchant)
    }

    @Test
    fun `parse handles missing date safely`() {
        val sms = "QL11223344 Confirmed. Ksh450.00 sent to JOHN DOE 0712345678."

        val parsed = MpesaSmsParser.parse(sms)

        assertNotNull(parsed)
        assertEquals("QL11223344", parsed?.mpesaCode)
        assertTrue((parsed?.date ?: 0L) > 0L)
    }
}
