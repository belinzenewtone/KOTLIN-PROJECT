package com.personal.lifeOS.platform.sms.parser

/**
 * Centralized M-Pesa parsing configuration with detection rules, confidence scoring,
 * and categorization.
 *
 * Rules are ordered by specificity: more specific patterns must come before general ones.
 * Example: "Reversal" must be checked before "Received" because a reversed received
 * transaction would match both patterns if checked in the wrong order.
 *
 * Each rule has:
 *  - patterns:            PRIMARY structural patterns (HIGH confidence on match)
 *  - fallbackPatterns:    MEDIUM confidence keyword fallbacks for regional/legacy variants
 *  - counterpartyPatterns: Ordered regexes to extract merchant/recipient name
 */
object MpesaParsingConfig {

    /**
     * Confidence levels assigned during parsing.
     *  HIGH   — matched a primary structural pattern (exact Safaricom template)
     *  MEDIUM — matched fallback/legacy pattern or last-resort keyword scan
     *  LOW    — reserved for future ambiguous review paths
     */
    enum class Confidence { HIGH, MEDIUM, LOW }

    /**
     * Semantic transaction categories for categorization and display.
     */
    enum class TransactionCategory {
        RECEIVED,    // Money received from person or bank
        SENT,        // Money sent to person (P2P)
        AIRTIME,     // Airtime purchase
        PAYBILL,     // Paybill payment (electricity, water, etc.)
        BUY_GOODS,   // Buy goods (merchant till payment)
        DEPOSIT,     // Agent/bank deposit
        WITHDRAW,    // ATM / agent withdrawal
        REVERSED,    // Reversed transaction
        LOAN,        // Fuliza loan repayment
        UNKNOWN,
    }

    /**
     * Detection rules ordered by specificity — the classifier short-circuits on the
     * first match. Rules that share keywords with more general rules MUST come first.
     *
     * Order rationale:
     *  1. Reversal    — contains "received" / "sent" keywords → must be first
     *  2. Received    — specific "received Ksh from" structure
     *  3. Deposit     — "deposited" keyword, distinct enough
     *  4. Airtime     — "for airtime" → must come BEFORE paybill ("sent to ... for airtime")
     *  5. Paybill     — "sent to ... account XXX" → must come BEFORE buy_goods
     *  6. Buy goods   — "paid to ..." without account number
     *  7. Withdrawal  — "withdrawn" keyword
     *  8. Fuliza      — "from your M-PESA ... Fuliza" → before generic "sent to"
     *  9. Sent P2P    — last; catches all remaining "sent to" after above are handled
     */
    val DETECTION_RULES: List<DetectionRule> = listOf(

        // ── 1. Reversal ──────────────────────────────────────────────────────────
        DetectionRule(
            id = "reversal",
            category = TransactionCategory.REVERSED,
            description = "Reversal of previous transaction",
            patterns = listOf(
                Regex("(?:transaction of|transaction for)\\s*(?:Ksh|KES)\\s?[\\d,.]+.*?has been reversed", RegexOption.IGNORE_CASE),
                Regex("(?:received|sent)\\s+(?:Ksh|KES)\\s?[\\d,.]+.+has been reversed", RegexOption.IGNORE_CASE),
                // "Ksh500 sent to JOHN…has been reversed" — MUST come before sent_p2p rule
                Regex("(?:Ksh|KES)\\s?[\\d,.]+\\s+sent to.+has been reversed", RegexOption.IGNORE_CASE),
                // "Ksh500 received from JOHN…has been reversed" — MUST come before received rule
                Regex("(?:Ksh|KES)\\s?[\\d,.]+\\s+received from.+has been reversed", RegexOption.IGNORE_CASE),
            ),
            fallbackPatterns = listOf(
                Regex("has been reversed", RegexOption.IGNORE_CASE),
                Regex("transaction.*reversed", RegexOption.IGNORE_CASE),
            ),
            counterpartyPatterns = listOf(
                Regex("received from\\s+(.+?)(?:\\s+on\\s|\\s+New\\s|\\.|$)", RegexOption.IGNORE_CASE),
                Regex("sent to\\s+(.+?)(?:\\s+on\\s|\\s+New\\s|\\.|$)", RegexOption.IGNORE_CASE),
            ),
            confidence = Confidence.HIGH,
        ),

        // ── 2. Received (P2P credit) ─────────────────────────────────────────────
        DetectionRule(
            id = "received",
            category = TransactionCategory.RECEIVED,
            description = "Money received from person or bank",
            patterns = listOf(
                // "You have received Ksh390.00 from JOHN DOE …"
                Regex("(?:you have\\s+)?received\\s+(?:Ksh|KES)\\s?[\\d,.]+\\s+from\\s+", RegexOption.IGNORE_CASE),
                // "Ksh390.00 received from JOHN DOE …"
                Regex("(?:Ksh|KES)\\s?[\\d,.]+\\s+received from\\s+", RegexOption.IGNORE_CASE),
            ),
            fallbackPatterns = listOf(
                Regex("received from\\s+[A-Z]", RegexOption.IGNORE_CASE),
            ),
            counterpartyPatterns = listOf(
                Regex("received\\s+(?:Ksh|KES)\\s?[\\d,.]+\\s+from\\s+(.+?)(?:\\s+on\\s|\\s+New\\s|\\.|$)", RegexOption.IGNORE_CASE),
                Regex("(?:Ksh|KES)\\s?[\\d,.]+\\s+received from\\s+(.+?)(?:\\s+on\\s|\\s+New\\s|\\.|$)", RegexOption.IGNORE_CASE),
            ),
            confidence = Confidence.HIGH,
        ),

        // ── 3. Deposit (agent / bank float) ──────────────────────────────────────
        DetectionRule(
            id = "deposit",
            category = TransactionCategory.DEPOSIT,
            description = "Cash or bank deposit",
            patterns = listOf(
                Regex("(?:Ksh|KES)\\s?[\\d,.]+\\s+deposited", RegexOption.IGNORE_CASE),
                Regex("cash deposit of\\s+(?:Ksh|KES)\\s?[\\d,.]+", RegexOption.IGNORE_CASE),
                Regex("deposited\\s+(?:Ksh|KES)\\s?[\\d,.]+", RegexOption.IGNORE_CASE),
            ),
            fallbackPatterns = listOf(
                Regex("deposited\\s+(?:Ksh|KES)", RegexOption.IGNORE_CASE),
                Regex("\\b deposited\\b", RegexOption.IGNORE_CASE),
            ),
            counterpartyPatterns = emptyList(),
            confidence = Confidence.HIGH,
        ),

        // ── 4. Airtime ── BEFORE paybill to catch "sent to 0712345678 for airtime" ──
        DetectionRule(
            id = "airtime",
            category = TransactionCategory.AIRTIME,
            description = "Airtime purchase",
            patterns = listOf(
                // "You bought Ksh5.00 of airtime on …"
                Regex("(?:you\\s+)?bought\\s+(?:Ksh|KES)\\s?[\\d,.]+\\s+of airtime", RegexOption.IGNORE_CASE),
                // "Ksh30.00 sent to 0712345678 for airtime on …"
                Regex("(?:Ksh|KES)\\s?[\\d,.]+\\s+sent to\\s+\\d{9,12}\\s+for airtime", RegexOption.IGNORE_CASE),
                Regex("for airtime(?:\\s+on|\\s+purchase|\\s+of|\\s*\\.)", RegexOption.IGNORE_CASE),
                Regex("airtime\\s+(?:purchase|of\\s+(?:Ksh|KES))", RegexOption.IGNORE_CASE),
                Regex("of airtime purchased", RegexOption.IGNORE_CASE),
            ),
            fallbackPatterns = listOf(
                Regex("for airtime", RegexOption.IGNORE_CASE),
                Regex("airtime purchase", RegexOption.IGNORE_CASE),
                Regex("bought\\s+(?:Ksh|KES)\\s?[\\d,.]+\\s+of airtime", RegexOption.IGNORE_CASE),
            ),
            counterpartyPatterns = listOf(
                Regex("sent to\\s+(.+?)\\s+for airtime", RegexOption.IGNORE_CASE),
            ),
            confidence = Confidence.HIGH,
        ),

        // ── 5. Paybill (utility / bill — has account number) ────────────────────
        DetectionRule(
            id = "paybill",
            category = TransactionCategory.PAYBILL,
            description = "Paybill payment (utility, subscription)",
            patterns = listOf(
                // "Ksh1,250.00 sent to KPLC PREPAID for account 998877 on …"
                Regex("(?:Ksh|KES)\\s?[\\d,.]+\\s+sent to\\s+.+?\\s+(?:for account|account)\\s+[\\w-]+", RegexOption.IGNORE_CASE),
                Regex("paid to\\s+.+?\\s+for account\\s+[\\w-]+", RegexOption.IGNORE_CASE),
                Regex("paybill", RegexOption.IGNORE_CASE),
            ),
            fallbackPatterns = listOf(
                Regex("(?:sent to|paid to)\\s+.+?\\s+account\\s+[\\w-]+", RegexOption.IGNORE_CASE),
            ),
            counterpartyPatterns = listOf(
                Regex("(?:Ksh|KES)\\s?[\\d,.]+\\s+sent to\\s+(.+?)\\s+(?:for account|account)\\s+[\\w-]+", RegexOption.IGNORE_CASE),
                Regex("paid to\\s+(.+?)\\s+(?:for account|account)\\s+[\\w-]+", RegexOption.IGNORE_CASE),
                Regex("sent to\\s+(.+?)\\s+(?:for account|account)\\s+[\\w-]+", RegexOption.IGNORE_CASE),
            ),
            confidence = Confidence.HIGH,
        ),

        // ── 6. Buy Goods (merchant till — no account number) ────────────────────
        DetectionRule(
            id = "buy_goods",
            category = TransactionCategory.BUY_GOODS,
            description = "Merchant till payment",
            patterns = listOf(
                // "Ksh450.00 paid to NAIVAS WESTLANDS on …" / "… confirmed"
                Regex("(?:Ksh|KES)\\s?[\\d,.]+\\s+paid to\\s+.+?\\s+(?:on\\s\\d|\\.\\s|confirmed)", RegexOption.IGNORE_CASE),
                // Kopo Kopo merchant payments
                Regex("(?:Ksh|KES)\\s?[\\d,.]+\\s+paid to\\s+.+?\\s+via\\s+kopo\\s+kopo", RegexOption.IGNORE_CASE),
                Regex("buy goods", RegexOption.IGNORE_CASE),
                Regex("till number\\s+\\d+", RegexOption.IGNORE_CASE),
                Regex("paid to\\s+[A-Z].+?\\.\\s+New M-PESA", RegexOption.IGNORE_CASE),
            ),
            fallbackPatterns = listOf(
                Regex("paid to\\s+[A-Z]", RegexOption.IGNORE_CASE),
            ),
            counterpartyPatterns = listOf(
                Regex("buy goods from\\s+(.+?)(?:\\s+on\\s|\\.|$)", RegexOption.IGNORE_CASE),
                Regex("(?:Ksh|KES)\\s?[\\d,.]+\\s+paid to\\s+(.+?)\\s+via\\s+kopo\\s+kopo(?:\\.\\s|\\s+on\\s|\\s+New\\s|$)", RegexOption.IGNORE_CASE),
                Regex("(?:Ksh|KES)\\s?[\\d,.]+\\s+paid to\\s+(.+?)(?:\\s+on\\s\\d|\\.\\s|confirmed|$)", RegexOption.IGNORE_CASE),
                Regex("paid to\\s+(.+?)(?:\\s+on\\s\\d|\\.\\s|confirmed|$)", RegexOption.IGNORE_CASE),
            ),
            confidence = Confidence.HIGH,
        ),

        // ── 7. Withdrawal ────────────────────────────────────────────────────────
        DetectionRule(
            id = "withdrawal",
            category = TransactionCategory.WITHDRAW,
            description = "ATM or agent cash withdrawal",
            patterns = listOf(
                Regex("withdrawn from agent", RegexOption.IGNORE_CASE),
                Regex("cash withdrawal\\s+of\\s+(?:Ksh|KES)", RegexOption.IGNORE_CASE),
                Regex("(?:Ksh|KES)\\s?[\\d,.]+\\s+withdrawn", RegexOption.IGNORE_CASE),
            ),
            fallbackPatterns = listOf(
                Regex("cash withdrawal", RegexOption.IGNORE_CASE),
                Regex("withdrawn from", RegexOption.IGNORE_CASE),
            ),
            counterpartyPatterns = listOf(
                Regex("withdrawn from(?:\\s+agent)?\\s+\\d+\\s*-?\\s*(.+?)(?:\\s+on\\s|\\s+New\\s|\\.|$)", RegexOption.IGNORE_CASE),
            ),
            confidence = Confidence.HIGH,
        ),

        // ── 8. Fuliza repayment (debt service) ── BEFORE sent_p2p ───────────────
        DetectionRule(
            id = "fuliza_repayment",
            category = TransactionCategory.LOAN,
            description = "Fuliza loan or repayment",
            patterns = listOf(
                // Exact Safaricom repayment template → HIGH confidence
                Regex("(?:Ksh|KES)\\s?[\\d,.]+\\s+from your M-PESA has been used to (?:partially|fully)\\s+pay your outstanding Fuliza M-PESA", RegexOption.IGNORE_CASE),
                Regex("(?:Ksh|KES)\\s?[\\d,.]+\\s+from your M-PESA has been used to .*outstanding Fuliza M-PESA", RegexOption.IGNORE_CASE),
            ),
            fallbackPatterns = listOf(
                // Looser Fuliza keyword match → MEDIUM confidence
                Regex("from your M-PESA has been used to .*Fuliza", RegexOption.IGNORE_CASE),
                Regex("outstanding Fuliza M-PESA", RegexOption.IGNORE_CASE),
                Regex("from your M-PESA.*Fuliza", RegexOption.IGNORE_CASE),
                Regex("M-PESA.*Fuliza", RegexOption.IGNORE_CASE),
            ),
            counterpartyPatterns = listOf(
                Regex("(Fuliza M-PESA)", RegexOption.IGNORE_CASE),
            ),
            confidence = Confidence.MEDIUM,
        ),

        // ── 9. Sent P2P ── LAST; catches remaining "sent to PERSON" ─────────────
        DetectionRule(
            id = "sent_p2p",
            category = TransactionCategory.SENT,
            description = "Peer-to-peer money transfer",
            patterns = listOf(
                // "Ksh390.00 sent to JANE DOE 0712345678 on …"
                Regex("(?:Ksh|KES)\\s?[\\d,.]+\\s+sent to\\s+[A-Z].+?(?:\\s+on\\s|\\s+New\\s|\\.)", RegexOption.IGNORE_CASE),
                Regex("customer transfer of\\s+(?:Ksh|KES)\\s?[\\d,.]+\\s+to\\s+", RegexOption.IGNORE_CASE),
            ),
            fallbackPatterns = listOf(
                Regex("sent to\\s+[A-Z]", RegexOption.IGNORE_CASE),
            ),
            counterpartyPatterns = listOf(
                Regex("(?:Ksh|KES)\\s?[\\d,.]+\\s+sent to\\s+(.+?)(?:\\s+on\\s|\\s+New\\s|\\.|confirmed|$)", RegexOption.IGNORE_CASE),
                Regex("customer transfer of\\s+(?:Ksh|KES)\\s?[\\d,.]+\\s+to\\s+(.+?)(?:\\s+on\\s|\\s+New\\s|\\.|confirmed|$)", RegexOption.IGNORE_CASE),
            ),
            confidence = Confidence.HIGH,
        ),
    )

    /**
     * Specific fee/notice signals that indicate a Fuliza service message (not a real transaction).
     * We require BOTH Fuliza context AND one of these fee signals to filter the message.
     * This prevents real Fuliza loan SMSes from being incorrectly ignored.
     */
    private val FULIZA_NOTICE_SIGNALS = listOf(
        "access fee charged",
        "outstanding amount is",
        "daily charges",
        "query charges",
        "select query charges",
        "interest accrual",
        "overdraft notice",
        "fuliza service charge",
    )

    /**
     * Returns true when the SMS is a Fuliza/M-Pesa service notice that should NOT be
     * imported as a transaction. Requires BOTH a Fuliza context word AND a specific fee
     * signal — this prevents real Fuliza transactions (which contain "Fuliza" but not fee
     * signals) from being accidentally filtered out.
     */
    fun isFulizaServiceNotice(message: String): Boolean {
        val text = message.lowercase().replace("\\s+".toRegex(), " ").trim()
        if (!text.contains("fuliza")) return false
        return FULIZA_NOTICE_SIGNALS.any { text.contains(it) }
    }

    /**
     * Human-readable display labels by category.
     */
    val CATEGORY_DISPLAY: Map<TransactionCategory, String> = mapOf(
        TransactionCategory.RECEIVED  to "M-Pesa Received",
        TransactionCategory.SENT      to "Transfer",
        TransactionCategory.AIRTIME   to "Airtime",
        TransactionCategory.PAYBILL   to "Utilities",
        TransactionCategory.BUY_GOODS to "Shopping",
        TransactionCategory.DEPOSIT   to "Deposit",
        TransactionCategory.WITHDRAW  to "Cash Withdrawal",
        TransactionCategory.REVERSED  to "Reversal",
        TransactionCategory.LOAN      to "Loans & Credit",
        TransactionCategory.UNKNOWN   to "Other",
    )
}

/**
 * A single detection rule used by the M-Pesa parser.
 *
 * @param id                  Stable identifier for debugging and logging
 * @param category            The [MpesaParsingConfig.TransactionCategory] this rule maps to
 * @param description         Human-readable description of the rule
 * @param patterns            PRIMARY patterns — structural match → HIGH confidence
 * @param fallbackPatterns    FALLBACK patterns — keyword match → MEDIUM confidence
 * @param counterpartyPatterns Ordered regexes for extracting the counterparty name
 * @param confidence          Base confidence for a primary pattern match
 */
data class DetectionRule(
    val id: String,
    val category: MpesaParsingConfig.TransactionCategory,
    val description: String,
    val patterns: List<Regex>,
    val fallbackPatterns: List<Regex> = emptyList(),
    val counterpartyPatterns: List<Regex> = emptyList(),
    val confidence: MpesaParsingConfig.Confidence,
)
