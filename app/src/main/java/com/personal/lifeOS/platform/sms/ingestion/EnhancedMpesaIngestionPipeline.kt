package com.personal.lifeOS.platform.sms.ingestion

import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import com.personal.lifeOS.platform.sms.audit.ImportAuditLogger
import com.personal.lifeOS.platform.sms.dedupe.MpesaDedupeEngineEnhanced
import com.personal.lifeOS.platform.sms.filter.ConfidenceBasedImportFilter
import com.personal.lifeOS.platform.sms.parser.MpesaParserEnhanced
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced M-Pesa ingestion pipeline with:
 * - 6-stage deterministic parsing with confidence scoring
 * - Dual-key deduplication (mpesa_code + source_hash)
 * - Confidence-based import filtering (realtime vs batch)
 * - Instant SMS processing via BroadcastReceiver (no polling)
 */
@Singleton
class EnhancedMpesaIngestionPipeline
    @Inject
    constructor(
        private val parserEnhanced: MpesaParserEnhanced,
        private val dedupeEngine: MpesaDedupeEngineEnhanced,
        private val expenseRepository: ExpenseRepository,
        private val importAuditLogger: ImportAuditLogger,
    ) {

        /**
         * Main ingestion method for realtime SMS receipt.
         * Implements the full 6-stage pipeline with confidence-based decisions.
         */
        suspend fun ingestRealtimeSms(
            rawMessage: String,
            source: MpesaIngestionSource,
        ): EnhancedMpesaIngestionOutcome {
            // Quick filter: is this even an M-Pesa message?
            if (!parserEnhanced.isMpesaSms(rawMessage)) {
                importAuditLogger.log(
                    outcome = "ignored_not_mpesa",
                    rawMessage = rawMessage,
                )
                return EnhancedMpesaIngestionOutcome.IGNORED_NOT_MPESA
            }

            // Parse through 6-stage pipeline (with confidence scoring)
            val parsed = parserEnhanced.parse(rawMessage)
            if (parsed == null) {
                importAuditLogger.log(
                    outcome = "parse_failed",
                    rawMessage = rawMessage,
                    failureReason = "Parser returned null",
                )
                return EnhancedMpesaIngestionOutcome.PARSE_FAILED
            }

            // Dual-key deduplication check
            if (dedupeEngine.isDuplicate(
                    mpesaCode = parsed.mpesaCode,
                    rawMessage = rawMessage,
                    amount = parsed.amount,
                    merchant = parsed.counterparty,
                    timestamp = parsed.date,
                )
            ) {
                importAuditLogger.log(
                    outcome = "duplicate_detected",
                    rawMessage = rawMessage,
                    mpesaCode = parsed.mpesaCode,
                    sourceHash = computeSourceHash(rawMessage),
                    amount = parsed.amount,
                    merchant = parsed.counterparty,
                )
                return EnhancedMpesaIngestionOutcome.DUPLICATE
            }

            // Confidence-based import decision
            val importDecision = ConfidenceBasedImportFilter.evaluateImportStrategy(parsed.confidence)

            // Try to import
            val imported = expenseRepository.importFromSms(rawMessage)
            if (imported == null) {
                importAuditLogger.log(
                    outcome = "import_failed",
                    rawMessage = rawMessage,
                    mpesaCode = parsed.mpesaCode,
                    amount = parsed.amount,
                    merchant = parsed.counterparty,
                    confidence = parsed.confidence.name,
                    importDecision = importDecision.name,
                    failureReason = "Repository import returned null",
                )
                return EnhancedMpesaIngestionOutcome.IMPORT_FAILED
            }

            // Log success with confidence level
            importAuditLogger.log(
                outcome = "imported_success",
                rawMessage = rawMessage,
                mpesaCode = parsed.mpesaCode,
                amount = parsed.amount,
                merchant = parsed.counterparty,
                category = imported.category,
                confidence = parsed.confidence.name,
                importDecision = importDecision.name,
                description = parsed.description,
            )

            // Return outcome that indicates confidence level
            return when (importDecision) {
                ConfidenceBasedImportFilter.ImportDecision.IMPORT_REALTIME ->
                    EnhancedMpesaIngestionOutcome.IMPORTED_REALTIME

                ConfidenceBasedImportFilter.ImportDecision.DEFER_TO_BATCH ->
                    EnhancedMpesaIngestionOutcome.IMPORTED_BATCH_PENDING

                ConfidenceBasedImportFilter.ImportDecision.QUARANTINE_FOR_REVIEW ->
                    EnhancedMpesaIngestionOutcome.IMPORTED_QUARANTINE
            }
        }

        /**
         * Compute SHA-256 hash of raw message for deduplication.
         */
        private fun computeSourceHash(rawMessage: String): String {
            return try {
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                val hashBytes = digest.digest(rawMessage.toByteArray(Charsets.UTF_8))
                hashBytes.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                rawMessage.hashCode().toString()
            }
        }
    }

/**
 * Outcomes from enhanced ingestion pipeline.
 */
enum class EnhancedMpesaIngestionOutcome {
    // Ignored messages (not M-Pesa, Fuliza notices, etc.)
    IGNORED_NOT_MPESA,

    // Parsing failures
    PARSE_FAILED,

    // Duplicates
    DUPLICATE,

    // Import failures
    IMPORT_FAILED,

    // Successful imports with confidence levels
    IMPORTED_REALTIME,        // HIGH confidence → shown immediately
    IMPORTED_BATCH_PENDING,   // MEDIUM confidence → deferred to batch
    IMPORTED_QUARANTINE,      // LOW confidence → requires manual review
}
