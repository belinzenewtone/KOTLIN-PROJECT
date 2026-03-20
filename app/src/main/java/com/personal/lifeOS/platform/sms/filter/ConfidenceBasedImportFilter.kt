package com.personal.lifeOS.platform.sms.filter

import com.personal.lifeOS.platform.sms.parser.MpesaParsingConfig

/**
 * Confidence-based filtering for M-Pesa import decisions.
 *
 * Strategy:
 * - HIGH confidence: import immediately to realtime feed
 * - MEDIUM confidence: defer to batch reconciliation (manual review)
 * - LOW confidence: quarantine for later inspection
 *
 * This ensures instant accuracy without false-positive cluttering of the realtime feed.
 */
object ConfidenceBasedImportFilter {

    /**
     * Determine whether to import a transaction in realtime or defer to batch.
     *
     * Returns ImportDecision which tells the ingestion pipeline what to do.
     */
    fun evaluateImportStrategy(confidence: MpesaParsingConfig.Confidence): ImportDecision {
        return when (confidence) {
            MpesaParsingConfig.Confidence.HIGH -> ImportDecision.IMPORT_REALTIME
            MpesaParsingConfig.Confidence.MEDIUM -> ImportDecision.DEFER_TO_BATCH
            MpesaParsingConfig.Confidence.LOW -> ImportDecision.QUARANTINE_FOR_REVIEW
        }
    }

    /**
     * Represents a decision about when/how to import a transaction.
     */
    enum class ImportDecision {
        /**
         * Import immediately to the realtime transaction feed.
         * User sees this transaction instantly, with full confidence in accuracy.
         */
        IMPORT_REALTIME,

        /**
         * Defer to batch reconciliation (e.g., nightly or weekly).
         * Transaction is saved but not shown in realtime feed until batch verification.
         * Batching allows manual review and correction before user sees it.
         */
        DEFER_TO_BATCH,

        /**
         * Quarantine for manual review.
         * Transaction is saved but hidden until user explicitly reviews it.
         * Use for truly ambiguous messages that need human decision.
         */
        QUARANTINE_FOR_REVIEW,
    }

    /**
     * Check if a transaction should be shown in the realtime feed.
     */
    fun shouldShowInRealtimeFeed(decision: ImportDecision): Boolean {
        return decision == ImportDecision.IMPORT_REALTIME
    }

    /**
     * Check if a transaction should be included in batch reconciliation.
     */
    fun shouldIncludeInBatchReconciliation(decision: ImportDecision): Boolean {
        return decision == ImportDecision.DEFER_TO_BATCH
    }

    /**
     * Check if a transaction requires manual review.
     */
    fun requiresManualReview(decision: ImportDecision): Boolean {
        return decision == ImportDecision.QUARANTINE_FOR_REVIEW
    }
}
