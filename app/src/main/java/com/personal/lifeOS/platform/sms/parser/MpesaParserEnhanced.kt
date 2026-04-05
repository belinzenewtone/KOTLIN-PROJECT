package com.personal.lifeOS.platform.sms.parser

import java.util.Locale
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * M-Pesa SMS Parser — Reliability-First Edition
 *
 * Architecture
 * ────────────
 *  Stage 0  Fast filter  (is this even an M-Pesa SMS?)
 *  Stage 1  Extract transaction code (required — no code = not a valid tx)
 *  Stage 2  Extract amount (required — zero / negative = invalid)
 *  Stage 3  Classify transaction intent via DETECTION_RULES (3-phase)
 *  Stage 4  Extract counterparty / merchant / account / agent
 *  Stage 5  Assign confidence score
 *  Stage 6  Build human-readable description
 *
 * Confidence contract
 * ───────────────────
 *  HIGH   — primary structural pattern matched (exact Safaricom template)
 *  MEDIUM — fallback keyword pattern or last-resort keyword scan matched
 *
 * Direction guarantee
 * ───────────────────
 *  TransactionCategory is ALWAYS derived from DETECTION_RULES, never from
 *  merchant guesses or isolated keyword heuristics.
 *
 * Battery efficiency
 * ──────────────────
 *  All operations are purely in-memory Regex operations with no I/O.
 *  Designed for use in a foreground Service or WorkManager task that only
 *  wakes when the device receives a new SMS broadcast.
 */
object MpesaParserEnhanced {

    // ── Public types ──────────────────────────────────────────────────────────

    /**
     * A successfully parsed M-Pesa transaction with full metadata.
     */
    data class ParsedTransaction(
        val mpesaCode: String,
        val amount: Double,
        val category: MpesaParsingConfig.TransactionCategory,
        val confidence: MpesaParsingConfig.Confidence,
        /** Merchant, recipient, biller, or agent name — null if not extractable. */
        val counterparty: String?,
        /** Human-readable description for display, e.g. "Sent to JOHN DOE". */
        val description: String,
        /** Balance after the transaction (if present in SMS). */
        val balanceAfter: Double?,
        val date: Long,
        val rawSms: String,
    ) {
        /**
         * True when money credited into the M-Pesa wallet:
         *  RECEIVED — someone sent you money (P2P credit)
         *  DEPOSIT  — agent or bank deposited cash into your wallet
         *
         * REVERSED is intentionally excluded — a reversal may return money to
         * the wallet, but the original direction is already recorded. Callers
         * should handle REVERSED separately if they need a net-zero adjustment.
         */
        val isIncome: Boolean
            get() = category == MpesaParsingConfig.TransactionCategory.RECEIVED ||
                category == MpesaParsingConfig.TransactionCategory.DEPOSIT

        /**
         * True when money debited from the M-Pesa wallet:
         *  SENT      — P2P transfer to another person
         *  AIRTIME   — prepaid airtime or data bundle purchase
         *  PAYBILL   — utility/subscription payment via Paybill
         *  BUY_GOODS — merchant till (Lipa na M-Pesa)
         *  WITHDRAW  — cash withdrawal from agent or ATM
         *
         * REVERSED and UNKNOWN are excluded — their monetary impact is ambiguous.
         */
        val isExpense: Boolean
            get() = when (category) {
                MpesaParsingConfig.TransactionCategory.SENT,
                MpesaParsingConfig.TransactionCategory.AIRTIME,
                MpesaParsingConfig.TransactionCategory.PAYBILL,
                MpesaParsingConfig.TransactionCategory.BUY_GOODS,
                MpesaParsingConfig.TransactionCategory.WITHDRAW,
                -> true
                else -> false
            }
    }

    // ── Extraction regexes ────────────────────────────────────────────────────

    /** M-Pesa transaction codes: exactly 10 uppercase alphanumeric at the START. */
    private val CODE_RE = Regex("^([A-Z0-9]{10})\\b")

    /** Amount: "Ksh 1,234.56", "KES1234", "Ksh390.00" etc. */
    private val AMOUNT_RE = Regex("(?:Ksh|KES)\\s?([\\d,]+(?:\\.\\d{1,2})?)", RegexOption.IGNORE_CASE)

    /** Balance after transaction. */
    private val BALANCE_RE = Regex(
        "(?:New M-PESA balance is|balance is|M-PESA balance is)\\s*(?:Ksh|KES)\\s?([\\d,]+(?:\\.\\d{1,2})?)",
        RegexOption.IGNORE_CASE,
    )

    /** Date and optional time inside the SMS body. */
    private val DATE_RE = Regex(
        "(\\d{1,2}/\\d{1,2}/\\d{2,4})(?:\\s+at\\s+(\\d{1,2}:\\d{2}\\s*[AP]M))?",
        RegexOption.IGNORE_CASE,
    )

    private val SUPPORTED_DATE_FORMATTERS = listOf(
        DateTimeFormatter.ofPattern("d/M/yy h:mm a", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d/M/yyyy h:mm a", Locale.ENGLISH),
    )

    // ── Stage 0: Fast filter ──────────────────────────────────────────────────

    /**
     * Returns true when [sms] looks like an M-Pesa transaction or notice message.
     * Cheap pre-check — the full parse pipeline applies additional validation.
     */
    fun isMpesaSms(sms: String): Boolean {
        val upper = sms.uppercase()
        return upper.contains("MPESA") ||
            upper.contains("M-PESA") ||
            (CODE_RE.containsMatchIn(sms.trim()) && AMOUNT_RE.containsMatchIn(sms))
    }

    // ── Stage 3: Rule classification ─────────────────────────────────────────

    private data class MatchResult(
        val rule: DetectionRule,
        val confidence: MpesaParsingConfig.Confidence,
    )

    /**
     * 3-phase rule detection:
     *  Phase 1 — Primary structural patterns → HIGH confidence
     *  Phase 2 — Fallback keyword patterns   → MEDIUM confidence
     *  Phase 3 — Last-resort keyword scan    → MEDIUM confidence
     */
    private fun detectRule(body: String): MatchResult? {
        // Phase 1: Primary structural match
        for (rule in MpesaParsingConfig.DETECTION_RULES) {
            if (rule.patterns.any { it.containsMatchIn(body) }) {
                return MatchResult(rule, MpesaParsingConfig.Confidence.HIGH)
            }
        }

        // Phase 2: Fallback keyword match
        for (rule in MpesaParsingConfig.DETECTION_RULES) {
            if (rule.fallbackPatterns.any { it.containsMatchIn(body) }) {
                return MatchResult(rule, MpesaParsingConfig.Confidence.MEDIUM)
            }
        }

        // Phase 3: Last-resort keyword scan for very old or unusual formats
        val text = body.lowercase()
        val lastResortId: String? = when {
            text.contains("has been reversed")                               -> "reversal"
            text.contains(" deposited") || text.contains("cash deposit")     -> "deposit"
            text.contains("for airtime") || (text.contains("bought") && text.contains("airtime")) -> "airtime"
            (text.contains("sent to") || text.contains("paid to")) &&
                (text.contains(" account ") || text.contains("for account")) -> "paybill"
            text.contains("paid to")                                         -> "buy_goods"
            text.contains("withdrawn from agent") || text.contains("cash withdrawal") -> "withdrawal"
            text.contains("from your m-pesa has been used to") &&
                text.contains("outstanding fuliza")                         -> "fuliza_repayment"
            text.contains("received from") || text.contains("you have received") -> "received"
            text.contains("sent to") || text.contains("customer transfer")  -> "sent_p2p"
            else -> null
        }

        return lastResortId?.let { id ->
            MpesaParsingConfig.DETECTION_RULES.find { it.id == id }
                ?.let { MatchResult(it, MpesaParsingConfig.Confidence.MEDIUM) }
        }
    }

    // ── Stage 4: Counterparty extraction ─────────────────────────────────────

    private fun extractCounterparty(body: String, rule: DetectionRule): String? {
        for (pattern in rule.counterpartyPatterns) {
            val candidate = pattern.find(body)?.groupValues?.getOrNull(1)
            if (!candidate.isNullOrBlank()) {
                return cleanCounterparty(candidate)
            }
        }
        // Sensible defaults for types with no extractable counterparty
        return when (rule.category) {
            MpesaParsingConfig.TransactionCategory.DEPOSIT  -> "Cash Deposit"
            MpesaParsingConfig.TransactionCategory.AIRTIME  -> "Airtime Purchase"
            MpesaParsingConfig.TransactionCategory.WITHDRAW -> "ATM Withdrawal"
            else -> null
        }
    }

    /** Remove trailing noise and whitespace from extracted counterparty strings. */
    private fun cleanCounterparty(value: String): String? {
        return value
            .replace("\\s+".toRegex(), " ")
            .replace("\\s+\\d{9,12}$".toRegex(), "")
            .replace("\\s+via\\s+kopo\\s+kopo.*$".toRegex(RegexOption.IGNORE_CASE), "")
            .trimEnd('.')
            .replace("\\s+New M-PESA.*$".toRegex(RegexOption.IGNORE_CASE), "")
            .trim()
            .takeIf { it.isNotBlank() && !it.startsWith("on ", ignoreCase = true) }
    }

    // ── Stage 6: Description builder ─────────────────────────────────────────

    private fun buildDescription(
        category: MpesaParsingConfig.TransactionCategory,
        counterparty: String?,
        amount: Double,
    ): String = when (category) {
        MpesaParsingConfig.TransactionCategory.RECEIVED  ->
            if (counterparty != null) "Received from $counterparty" else "M-Pesa received"
        MpesaParsingConfig.TransactionCategory.DEPOSIT   -> "Cash deposit"
        MpesaParsingConfig.TransactionCategory.AIRTIME   ->
            if (counterparty != null && counterparty != "Airtime Purchase") "Airtime for $counterparty"
            else "Airtime purchase"
        MpesaParsingConfig.TransactionCategory.LOAN      -> "Fuliza repayment"
        MpesaParsingConfig.TransactionCategory.PAYBILL   ->
            if (counterparty != null) "Paid to $counterparty (Paybill)" else "Paybill payment"
        MpesaParsingConfig.TransactionCategory.BUY_GOODS ->
            if (counterparty != null) "Bought goods at $counterparty" else "Buy Goods"
        MpesaParsingConfig.TransactionCategory.WITHDRAW  ->
            if (counterparty != null && counterparty != "ATM Withdrawal") "Withdrawal at $counterparty"
            else "Cash withdrawal"
        MpesaParsingConfig.TransactionCategory.REVERSED  ->
            if (counterparty != null) "Reversal for $counterparty" else "Transaction reversed"
        MpesaParsingConfig.TransactionCategory.SENT      ->
            if (counterparty != null) "Sent to $counterparty" else "M-Pesa sent"
        MpesaParsingConfig.TransactionCategory.UNKNOWN   ->
            "M-Pesa KES ${String.format(Locale.US, "%,.0f", amount)}"
    }

    // ── Auxiliary extractors ──────────────────────────────────────────────────

    private fun parseAmount(body: String): Double? {
        val num = AMOUNT_RE.find(body)
            ?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?: return null
        return if (num > 0) num else null
    }

    private fun parseBalance(body: String): Double? {
        return BALANCE_RE.find(body)
            ?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
    }

    private fun parseDate(body: String): Long {
        return try {
            val dateMatch = DATE_RE.find(body) ?: return System.currentTimeMillis()
            val datePart = dateMatch.groupValues[1]
            val timePart = dateMatch.groupValues.getOrNull(2)
                ?.takeIf { it.isNotBlank() } ?: "12:00 PM"
            val combined = "$datePart $timePart"
            SUPPORTED_DATE_FORMATTERS.firstNotNullOfOrNull { formatter ->
                runCatching {
                    LocalDateTime.parse(combined, formatter)
                        .atZone(ZoneId.systemDefault())
                        .toInstant().toEpochMilli()
                }.getOrNull()
            } ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Parse a single M-Pesa SMS body through the 6-stage pipeline.
     *
     * Returns a [ParsedTransaction] on success, or null when the message:
     *  - Has no valid 10-character M-Pesa code
     *  - Contains no parseable positive amount
     *  - Is a Fuliza service/fee notice (not a real transaction)
     *  - Has an unrecognised format that cannot be safely classified
     *
     * This method is thread-safe and performs no I/O — safe to call on any
     * thread without impacting battery consumption.
     */
    fun parse(sms: String): ParsedTransaction? {
        return try {
            val normalized = sms.replace("\\s+".toRegex(), " ").trim()

            // Stage 0: Filter Fuliza service/fee notices — NOT real transactions
            if (MpesaParsingConfig.isFulizaServiceNotice(normalized)) return null

            // Stage 1: M-Pesa code is required
            val code = CODE_RE.find(normalized)?.groupValues?.get(1) ?: return null

            // Stage 2: Positive amount is required
            val amount = parseAmount(normalized) ?: return null

            // Stage 3: Classify
            val match = detectRule(normalized) ?: return null

            // Stage 4: Counterparty
            val counterparty = extractCounterparty(normalized, match.rule)

            // Stage 5: Balance (optional enrichment)
            val balance = parseBalance(normalized)

            // Stage 6: Description
            val description = buildDescription(match.rule.category, counterparty, amount)

            ParsedTransaction(
                mpesaCode    = code,
                amount       = amount,
                category     = match.rule.category,
                confidence   = match.confidence,
                counterparty = counterparty,
                description  = description,
                balanceAfter = balance,
                date         = parseDate(normalized),
                rawSms       = sms,
            )
        } catch (e: Exception) {
            null
        }
    }
}

