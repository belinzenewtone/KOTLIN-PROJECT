package com.personal.lifeOS.platform.sms.parser

import com.personal.lifeOS.platform.sms.parser.MpesaParsingConfig.TransactionCategory

data class MpesaParserFixture(
    val name: String,
    val sms: String,
    val code: String,
    val amount: Double,
    val merchant: String,
    val category: TransactionCategory,
)

object MpesaParserFixtures {
    val entries =
        listOf(
            MpesaParserFixture(
                name = "sent_to_contact",
                sms = "QH12345678 Confirmed. Ksh1,250.00 sent to JOHN DOE 0712345678 on 7/3/26 at 10:15 AM.",
                code = "QH12345678",
                amount = 1250.0,
                merchant = "JOHN DOE",
                category = TransactionCategory.SENT,
            ),
            MpesaParserFixture(
                name = "received_from_contact",
                sms =
                    "QJ22334455 Confirmed. You have received Ksh2,300.00 from JANE DOE 0712000000 " +
                        "on 14/3/26 at 8:45 AM.",
                code = "QJ22334455",
                amount = 2300.0,
                merchant = "JANE DOE",
                category = TransactionCategory.RECEIVED,
            ),
            MpesaParserFixture(
                name = "lipa_na_mpesa_paid_to_till",
                sms =
                    "QK99887766 Confirmed. Ksh540.00 paid to NAIVAS WESTLANDS on 18/3/26 at 7:20 PM. " +
                        "New M-PESA balance is Ksh1,200.00.",
                code = "QK99887766",
                amount = 540.0,
                merchant = "NAIVAS WESTLANDS",
                category = TransactionCategory.BUY_GOODS,
            ),
            MpesaParserFixture(
                name = "buy_goods_from_merchant",
                sms = "QN55443322 Confirmed. Ksh799.00 Buy Goods from JAVA HOUSE KAREN on 16/3/26 at 12:05 PM.",
                code = "QN55443322",
                amount = 799.0,
                merchant = "JAVA HOUSE KAREN",
                category = TransactionCategory.BUY_GOODS,
            ),
            MpesaParserFixture(
                name = "atm_withdrawal",
                sms = "QL33445566 Confirmed. Ksh3,000.00 withdrawn from Agent 123456 on 15/3/26 at 9:10 AM.",
                code = "QL33445566",
                amount = 3000.0,
                merchant = "ATM Withdrawal",
                category = TransactionCategory.WITHDRAW,
            ),
            MpesaParserFixture(
                name = "airtime_purchase",
                sms = "QM77665544 Confirmed. You bought Ksh50.00 of airtime on 12/3/26 at 11:30 AM.",
                code = "QM77665544",
                amount = 50.0,
                merchant = "Airtime Purchase",
                category = TransactionCategory.AIRTIME,
            ),
        )
}
