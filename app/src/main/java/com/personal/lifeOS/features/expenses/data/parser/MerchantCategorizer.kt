package com.personal.lifeOS.features.expenses.data.parser

/**
 * Auto-categorizes merchants based on known patterns.
 * Falls back to user-corrected categories stored in the merchant_categories table.
 */
object MerchantCategorizer {

    enum class ExpenseCategory(val label: String) {
        FOOD("Food"),
        TRANSPORT("Transport"),
        BILLS("Bills"),
        SHOPPING("Shopping"),
        ENTERTAINMENT("Entertainment"),
        SUBSCRIPTIONS("Subscriptions"),
        SAVINGS("Savings"),
        GROCERIES("Groceries"),
        AIRTIME("Airtime"),
        OTHER("Other")
    }

    // Known merchant → category mappings for Kenya
    private val KNOWN_MERCHANTS = mapOf(
        // Food & Restaurants
        "KFC" to ExpenseCategory.FOOD,
        "JAVA" to ExpenseCategory.FOOD,
        "CHICKEN INN" to ExpenseCategory.FOOD,
        "PIZZA INN" to ExpenseCategory.FOOD,
        "DOMINOS" to ExpenseCategory.FOOD,
        "SUBWAY" to ExpenseCategory.FOOD,
        "MCDONALDS" to ExpenseCategory.FOOD,
        "ARTCAFFE" to ExpenseCategory.FOOD,
        "BIG SQUARE" to ExpenseCategory.FOOD,
        "CAFE DELI" to ExpenseCategory.FOOD,

        // Groceries / Supermarkets
        "NAIVAS" to ExpenseCategory.GROCERIES,
        "QUICKMART" to ExpenseCategory.GROCERIES,
        "CARREFOUR" to ExpenseCategory.GROCERIES,
        "CLEANSHELF" to ExpenseCategory.GROCERIES,
        "TUSKYS" to ExpenseCategory.GROCERIES,
        "CHANDARANA" to ExpenseCategory.GROCERIES,

        // Transport
        "UBER" to ExpenseCategory.TRANSPORT,
        "BOLT" to ExpenseCategory.TRANSPORT,
        "LITTLE" to ExpenseCategory.TRANSPORT,
        "SWVL" to ExpenseCategory.TRANSPORT,
        "KENYA RAILWAYS" to ExpenseCategory.TRANSPORT,
        "SGR" to ExpenseCategory.TRANSPORT,
        "SHELL" to ExpenseCategory.TRANSPORT,
        "TOTAL" to ExpenseCategory.TRANSPORT,
        "RUBIS" to ExpenseCategory.TRANSPORT,

        // Bills
        "KPLC" to ExpenseCategory.BILLS,
        "KENYA POWER" to ExpenseCategory.BILLS,
        "NAIROBI WATER" to ExpenseCategory.BILLS,
        "ZUKU" to ExpenseCategory.BILLS,
        "SAFARICOM" to ExpenseCategory.BILLS,
        "AIRTEL" to ExpenseCategory.BILLS,
        "TELKOM" to ExpenseCategory.BILLS,
        "DSTV" to ExpenseCategory.BILLS,
        "GOTV" to ExpenseCategory.BILLS,
        "STARTIMES" to ExpenseCategory.BILLS,

        // Subscriptions
        "NETFLIX" to ExpenseCategory.SUBSCRIPTIONS,
        "SPOTIFY" to ExpenseCategory.SUBSCRIPTIONS,
        "YOUTUBE" to ExpenseCategory.SUBSCRIPTIONS,
        "SHOWMAX" to ExpenseCategory.SUBSCRIPTIONS,
        "APPLE" to ExpenseCategory.SUBSCRIPTIONS,

        // Entertainment
        "IMAX" to ExpenseCategory.ENTERTAINMENT,
        "ANGA" to ExpenseCategory.ENTERTAINMENT,
        "CENTURY CINEMAX" to ExpenseCategory.ENTERTAINMENT,

        // Shopping
        "JUMIA" to ExpenseCategory.SHOPPING,
        "KILIMALL" to ExpenseCategory.SHOPPING,
        "MASOKO" to ExpenseCategory.SHOPPING,
    )

    /**
     * Categorize a merchant name.
     * Uses fuzzy matching against known merchants.
     */
    fun categorize(merchantName: String): ExpenseCategory {
        val upper = merchantName.uppercase().trim()

        // Direct match
        KNOWN_MERCHANTS[upper]?.let { return it }

        // Partial match — check if the merchant name contains a known merchant
        for ((knownMerchant, category) in KNOWN_MERCHANTS) {
            if (upper.contains(knownMerchant) || knownMerchant.contains(upper)) {
                return category
            }
        }

        // Keyword-based fallback
        return when {
            upper.contains("RESTAURANT") || upper.contains("CAFE") || upper.contains("HOTEL") -> ExpenseCategory.FOOD
            upper.contains("SUPERMARKET") || upper.contains("MART") -> ExpenseCategory.GROCERIES
            upper.contains("PHARMACY") || upper.contains("CHEMIST") -> ExpenseCategory.BILLS
            upper.contains("FUEL") || upper.contains("PETROL") -> ExpenseCategory.TRANSPORT
            upper.contains("AIRTIME") -> ExpenseCategory.AIRTIME
            else -> ExpenseCategory.OTHER
        }
    }
}
