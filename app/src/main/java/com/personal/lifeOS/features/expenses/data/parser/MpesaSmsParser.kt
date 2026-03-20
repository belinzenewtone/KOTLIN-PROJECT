package com.personal.lifeOS.features.expenses.data.parser

import com.personal.lifeOS.platform.sms.parser.MpesaParserEnhanced
import com.personal.lifeOS.platform.sms.parser.MpesaParsingConfig

/**
 * M-Pesa SMS parser — public facade over [MpesaParserEnhanced].
 *
 * This object provides the app-layer API used by [ExpenseRepositoryImpl] and tests.
 * All classification and extraction logic lives in [MpesaParserEnhanced]; this facade
 * maps the richer enhanced result into the leaner [ParsedTransaction] shape that the
 * repository expects.
 *
 * Breaking change from the previous version
 * ──────────────────────────────────────────
 * The inner [TransactionType] enum has been removed. Transaction semantics are now
 * expressed through [MpesaParsingConfig.TransactionCategory], which is strictly more
 * expressive:
 *
 *   Old                  New
 *   ────────────────────────────────────────
 *   TransactionType.PAID → PAYBILL or BUY_GOODS (correctly disambiguated)
 *   TransactionType.WITHDRAWN → WITHDRAW
 *   TransactionType.UNKNOWN → UNKNOWN
 *   … etc.
 */
object MpesaSmsParser {

    /**
     * A parsed M-Pesa transaction ready for persistence.
     *
     * @param mpesaCode       10-character Safaricom transaction reference
     * @param amount          Transaction amount (always positive)
     * @param merchant        Counterparty / merchant name extracted from the SMS
     * @param category        Semantic transaction category (replaces old TransactionType)
     * @param confidence      Parser confidence for this classification
     * @param date            Transaction epoch-millis parsed from the SMS body
     * @param rawSms          Original unmodified SMS body for audit / hashing
     */
    data class ParsedTransaction(
        val mpesaCode: String,
        val amount: Double,
        val merchant: String,
        val category: MpesaParsingConfig.TransactionCategory,
        val confidence: MpesaParsingConfig.Confidence,
        val date: Long,
        val rawSms: String,
    )

    /**
     * Returns true when [message] is an M-Pesa transaction or notice SMS.
     * Cheap pre-filter — use before calling [parse] on large inbox batches.
     */
    fun isMpesaSms(message: String): Boolean = MpesaParserEnhanced.isMpesaSms(message)

    /**
     * Parse an M-Pesa SMS and return a [ParsedTransaction], or null when:
     *  - The message is not M-Pesa
     *  - No valid 10-char transaction code is found
     *  - No positive amount is found
     *  - The message is a Fuliza service/fee notice (not a real transaction)
     *  - The transaction type cannot be reliably classified
     */
    fun parse(sms: String): ParsedTransaction? {
        val enhanced = MpesaParserEnhanced.parse(sms) ?: return null

        // Derive a usable merchant/counterparty string from the enhanced result.
        // Priority: extracted counterparty → category display label
        val merchant = enhanced.counterparty
            ?: MpesaParsingConfig.CATEGORY_DISPLAY[enhanced.category]
            ?: enhanced.category.name.lowercase().replaceFirstChar { it.uppercase() }

        return ParsedTransaction(
            mpesaCode  = enhanced.mpesaCode,
            amount     = enhanced.amount,
            merchant   = merchant,
            category   = enhanced.category,
            confidence = enhanced.confidence,
            date       = enhanced.date,
            rawSms     = enhanced.rawSms,
        )
    }
}
