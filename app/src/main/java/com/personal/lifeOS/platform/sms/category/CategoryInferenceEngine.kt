package com.personal.lifeOS.platform.sms.category

import com.personal.lifeOS.platform.sms.parser.MpesaParsingConfig.TransactionCategory

/**
 * Offline category inference engine — no network, no AI required.
 *
 * Priority chain (first match wins):
 *  1. Learned merchant rule       — user-trained, always wins (caller's responsibility)
 *  2. M-Pesa transaction type     — semantic type from the parser
 *  3. Keyword rules               — 80+ Kenyan merchant/brand patterns
 *  4. Amount heuristic            — last resort
 *
 * Designed to correctly categorise >85% of real Kenyan M-Pesa transactions
 * without any manual input.
 */
object CategoryInferenceEngine {

    // ── Public types ──────────────────────────────────────────────────────────

    enum class InferenceSource {
        MPESA_KIND,        // Derived from M-Pesa transaction type
        KEYWORD,           // Matched a merchant keyword rule
        AMOUNT_HEURISTIC,  // Fallback: based on transaction amount only
    }

    data class InferredCategory(
        val category: String,
        val source: InferenceSource,
    )

    // ── Keyword rules ─────────────────────────────────────────────────────────
    //
    // Each rule: a regex matched against the normalised description / merchant
    // name, and the resulting category. More specific rules go first.

    private data class KeywordRule(val pattern: Regex, val category: String)

    private val KEYWORD_RULES: List<KeywordRule> = listOf(

        // ── Supermarkets & groceries ─────────────────────────────────────────
        KeywordRule(
            Regex("naivas|quickmart|carrefour|cleanshelf|eastmatt|tumaini|magunas?|food stall|mboga|mama mboga", RegexOption.IGNORE_CASE),
            "Groceries",
        ),
        KeywordRule(
            Regex("supermarket|minimart|groceries|kiosk|duka|farm fresh|green garden", RegexOption.IGNORE_CASE),
            "Groceries",
        ),

        // ── Restaurants & food delivery ──────────────────────────────────────
        KeywordRule(
            Regex("kfc|mcdonalds?|pizza ?inn|pizza ?hut|dominos?|chicken ?inn|creamy ?inn|galitos?|java|artcaff[eé]", RegexOption.IGNORE_CASE),
            "Eating Out",
        ),
        KeywordRule(
            Regex("cookiemans?|cookie ?man'?s|debonairs?|steers|nandos?|burger ?king|subway", RegexOption.IGNORE_CASE),
            "Eating Out",
        ),
        KeywordRule(
            Regex("jumia ?food|glovo|uber ?eats?|bolt ?food|yummy|mrdelivery|sendy", RegexOption.IGNORE_CASE),
            "Eating Out",
        ),
        KeywordRule(
            Regex("restaurant|hotel|cafe|cafeteria|canteen|eatery|food court|nyama choma|chips|ugali|chapati|mandazi", RegexOption.IGNORE_CASE),
            "Eating Out",
        ),

        // ── Transport ────────────────────────────────────────────────────────
        KeywordRule(
            Regex("uber|bolt|little ?cab|indriver|faras|taxify|taxi|cab|matatu|boda|tuk.?tuk|shuttle|bus fare", RegexOption.IGNORE_CASE),
            "Transport",
        ),
        KeywordRule(
            Regex("kenya ?airways?|jambojet|safari ?com ?air|flysax|airkenya|fly540|precision ?air", RegexOption.IGNORE_CASE),
            "Transport",
        ),
        KeywordRule(
            Regex("ntsa|parking|toll|e-citizen.*vehicle|sgr|rift valley railways", RegexOption.IGNORE_CASE),
            "Transport",
        ),

        // ── Fuel ─────────────────────────────────────────────────────────────
        KeywordRule(
            Regex("petrol|fuel|shell|total|kenol|rubis|vivo|oil ?lib|oilliby|kobil|hass petroleum", RegexOption.IGNORE_CASE),
            "Fuel",
        ),

        // ── Utilities ────────────────────────────────────────────────────────
        KeywordRule(
            Regex("kenya ?power|kplc|gotv|dstv|zuku|faiba|safaricom ?home|startimes?|poa ?internet", RegexOption.IGNORE_CASE),
            "Utilities",
        ),
        KeywordRule(
            Regex("nairobi ?water|nawasco|water|sewerage|nwsc|athi ?water", RegexOption.IGNORE_CASE),
            "Utilities",
        ),

        // ── Rent ─────────────────────────────────────────────────────────────
        KeywordRule(
            Regex("rent|landlord|caretaker|bedsitter|studio|one.?bedroom|two.?bedroom|airbnb|property", RegexOption.IGNORE_CASE),
            "Rent",
        ),

        // ── Telecoms & airtime ────────────────────────────────────────────────
        KeywordRule(
            Regex("safaricom|airtel|telkom|airtime|bundles?|data package|okoa.?jahazi|bonga|flex plan", RegexOption.IGNORE_CASE),
            "Airtime",
        ),

        // ── Health & pharmacy ─────────────────────────────────────────────────
        KeywordRule(
            Regex("pharmacy|chemist|hospital|clinic|doctor|daktari|medica|healthplus|goodlife|oasis|avenue hospital|aga khan|nairobi hospital|kenyatta hospital", RegexOption.IGNORE_CASE),
            "Health",
        ),
        KeywordRule(
            Regex("nhif|insurance|jubilee|britam|aar|madison|resolution|cic|old mutual insurance", RegexOption.IGNORE_CASE),
            "Insurance",
        ),

        // ── Education ────────────────────────────────────────────────────────
        KeywordRule(
            Regex("school fee|tuition|university|college|exam|knec|ucu|kenyatta univ|strathmore|usiu|moi university|daystar", RegexOption.IGNORE_CASE),
            "Education",
        ),
        KeywordRule(
            Regex("udemy|coursera|skillshare|youtube premium|spotify|netflix|showmax|apple tv|amazon prime", RegexOption.IGNORE_CASE),
            "Subscriptions",
        ),

        // ── Shopping & e-commerce ─────────────────────────────────────────────
        KeywordRule(
            Regex("jumia|kilimall|masoko|sky.?garden|mall|westgate|garden city|two rivers|junction|galleria|sarit|village market", RegexOption.IGNORE_CASE),
            "Shopping",
        ),
        KeywordRule(
            Regex("clothing|fashion|shoes|handbag|cosmetics|beauty|salon|barber|optical|glasses|phone shop|electronics", RegexOption.IGNORE_CASE),
            "Shopping",
        ),

        // ── Savings & investments ─────────────────────────────────────────────
        KeywordRule(
            Regex("m.?shwari|kcb.?m.?pesa|fuliza|tala|branch|zenka|berry|haraka|okolea|timiza|hustler fund", RegexOption.IGNORE_CASE),
            "Loans & Credit",
        ),
        KeywordRule(
            Regex("sacco|chama|merry.?go.?round|table banking|savings group|investment club", RegexOption.IGNORE_CASE),
            "Savings",
        ),
        KeywordRule(
            Regex("nse|cytonn|britam asset|cic asset|old mutual|stanbic|equity bank|kcb bank|coop bank|family bank|i&m|ncba", RegexOption.IGNORE_CASE),
            "Investments",
        ),

        // ── Entertainment ─────────────────────────────────────────────────────
        KeywordRule(
            Regex("cinema|movie|imax|centrepoint|century cinemas|entertainment|sport|gym|fitness|planet gym|cult fitness", RegexOption.IGNORE_CASE),
            "Entertainment",
        ),

        // ── Government & fees ─────────────────────────────────────────────────
        KeywordRule(
            Regex("county|nairobi city county|ecitizen|kra|tax|stamp duty|land rates|huduma", RegexOption.IGNORE_CASE),
            "Government Fees",
        ),
        KeywordRule(
            Regex("nssf|nhif|helb|student loan|government levy", RegexOption.IGNORE_CASE),
            "Government Fees",
        ),

        // ── Cash / Withdrawals ────────────────────────────────────────────────
        KeywordRule(
            Regex("atm|withdraw|cash out|agent|equity agent|kcb agent|co-op kwa jirani", RegexOption.IGNORE_CASE),
            "Cash Withdrawal",
        ),

        // ── P2P transfers ─────────────────────────────────────────────────────
        KeywordRule(
            Regex("sent to [0-9]{10}|send money|transfer to|western union|world remit|sendwave", RegexOption.IGNORE_CASE),
            "Transfer",
        ),
    )

    // ── Amount heuristic (last resort) ────────────────────────────────────────

    private fun amountHeuristic(amount: Double, isExpense: Boolean): String {
        if (!isExpense) {
            return if (amount >= 30_000) "Salary" else "M-Pesa Received"
        }
        return when {
            amount <= 50    -> "Airtime"
            amount <= 500   -> "Eating Out"
            amount <= 2_000 -> "Shopping"
            amount <= 8_000 -> "Utilities"
            else            -> "Transfer"
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Infer a category from the M-Pesa [transactionType], [description] / merchant
     * name, and [amount]. Does NOT check user-learned merchant rules — that is the
     * caller's responsibility (check [MerchantCategoryDao] first and skip inference
     * if a user-corrected rule exists).
     *
     * @param transactionType  Parsed M-Pesa category from [MpesaParsingConfig]
     * @param description      Merchant name / counterparty string (may be null)
     * @param amount           Transaction amount in KES
     */
    fun infer(
        transactionType: TransactionCategory,
        description: String?,
        amount: Double,
    ): InferredCategory {

        // Priority 2 — M-Pesa semantic type for unambiguous transaction kinds
        val kindCategory = when (transactionType) {
            TransactionCategory.SENT     -> "Transfer"
            TransactionCategory.RECEIVED -> "M-Pesa Received"
            TransactionCategory.AIRTIME  -> "Airtime"
            TransactionCategory.WITHDRAW -> "Cash Withdrawal"
            TransactionCategory.DEPOSIT  -> "Deposit"
            TransactionCategory.REVERSED -> "Reversal"
            TransactionCategory.LOAN     -> "Loans & Credit"
            TransactionCategory.FULIZA_CHARGE -> "Fuliza Charge"
            // BUY_GOODS, PAYBILL, UNKNOWN → fall through to keyword + heuristic
            TransactionCategory.BUY_GOODS,
            TransactionCategory.PAYBILL,
            TransactionCategory.UNKNOWN,
            -> null
        }
        if (kindCategory != null) {
            return InferredCategory(kindCategory, InferenceSource.MPESA_KIND)
        }

        // Priority 3 — Keyword rules (match against description/merchant name)
        val text = description.orEmpty().trim()
        if (text.isNotBlank()) {
            for (rule in KEYWORD_RULES) {
                if (rule.pattern.containsMatchIn(text)) {
                    return InferredCategory(rule.category, InferenceSource.KEYWORD)
                }
            }
        }

        // Priority 4 — Amount heuristic
        val isExpense = transactionType != TransactionCategory.RECEIVED &&
            transactionType != TransactionCategory.DEPOSIT &&
            transactionType != TransactionCategory.REVERSED
        return InferredCategory(
            category = amountHeuristic(amount, isExpense),
            source = InferenceSource.AMOUNT_HEURISTIC,
        )
    }

    /**
     * Normalise a raw merchant / counterparty name so it can be used as a
     * consistent lookup key (strip prefixes, phone numbers, noise words).
     */
    fun normalizeMerchantName(raw: String): String {
        return raw
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .replace(
                Regex("^(received from|sent to|paid to|bought goods at|withdrawal at|airtime for|reversal for)\\s+"),
                "",
            )
            .replace(Regex("\\s+(paybill|buy goods|m pesa|mpesa)\\b"), "")
            .replace(Regex("\\b(ksh|kes)\\b"), "")
            .replace(Regex("\\b\\d{7,}\\b"), "")  // strip phone/account numbers
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
