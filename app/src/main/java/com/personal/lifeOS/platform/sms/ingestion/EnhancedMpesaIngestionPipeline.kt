package com.personal.lifeOS.platform.sms.ingestion

import com.personal.lifeOS.core.preferences.AppSettingsStore
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import com.personal.lifeOS.features.expenses.domain.repository.FulizaLoanRepository
import com.personal.lifeOS.platform.sms.audit.ImportAuditLogger
import com.personal.lifeOS.platform.sms.dedupe.MpesaDedupeEngineEnhanced
import com.personal.lifeOS.platform.sms.filter.ConfidenceBasedImportFilter
import com.personal.lifeOS.platform.sms.parser.MpesaParserEnhanced
import com.personal.lifeOS.platform.sms.parser.MpesaParsingConfig.TransactionCategory
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
        private val fulizaLoanRepository: FulizaLoanRepository,
        private val importAuditLogger: ImportAuditLogger,
        private val appSettingsStore: AppSettingsStore,
    ) {

        /**
         * Main ingestion method for realtime SMS receipt.
         * Implements the full 6-stage pipeline with confidence-based decisions.
         */
        @Suppress("LongMethod")
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

            // ── FULIZA_CHARGE short-circuit ──────────────────────────────────────
            // Charge notice carries the authoritative total outstanding balance.
            // It is NOT a debit event — do not import as a transaction.
            // Template: "Fuliza M-PESA amount is Ksh X. … Total Fuliza M-PESA outstanding amount is Ksh Z"
            if (parsed.category == TransactionCategory.FULIZA_CHARGE) {
                val outstanding = parsed.fulizaOutstandingKes
                if (outstanding != null) {
                    fulizaLoanRepository.setOutstandingKes(outstanding)
                    importAuditLogger.log(
                        outcome = "fuliza_balance_updated",
                        rawMessage = rawMessage,
                        mpesaCode = parsed.mpesaCode,
                        amount = outstanding,
                        merchant = "Fuliza M-PESA",
                    )
                    return EnhancedMpesaIngestionOutcome.FULIZA_BALANCE_UPDATED
                }
                // Outstanding not extractable — fall through to normal import path
                // (this shouldn't happen for a well-formed charge notice, but be defensive)
            }

            // Dual-key deduplication check
            if (
                dedupeEngine.isDuplicate(
                    mpesaCode = parsed.mpesaCode,
                    rawMessage = rawMessage,
                    amount = parsed.amount,
                    merchant = parsed.counterparty ?: "Unknown",
                    timestamp = parsed.date,
                )
            ) {
                importAuditLogger.log(
                    outcome = "duplicate_detected",
                    rawMessage = rawMessage,
                    mpesaCode = parsed.mpesaCode,
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
                    failureReason = "Repository import returned null",
                )
                return EnhancedMpesaIngestionOutcome.IMPORT_FAILED
            }

            // Log success with confidence level
            importAuditLogger.log(
                outcome = importDecision.name.lowercase(),
                rawMessage = rawMessage,
                mpesaCode = parsed.mpesaCode,
                amount = parsed.amount,
                merchant = parsed.counterparty,
            )

            // ── Fuliza balance update from repayment SMS ─────────────────────────
            // Repayment SMS: "Ksh X from your M-PESA has been used to … pay your outstanding Fuliza …
            //                 Your available Fuliza M-PESA limit is Ksh Y."
            // outstanding = userFulizaLimit - availableLimit (both from SMS / settings)
            if (parsed.category == TransactionCategory.LOAN) {
                val availableLimit = parsed.fulizaAvailableLimitKes
                val userLimit = appSettingsStore.getFulizaLimitKes()
                if (availableLimit != null && userLimit != null && userLimit > 0.0) {
                    val outstanding = (userLimit - availableLimit).coerceAtLeast(0.0)
                    fulizaLoanRepository.setOutstandingKes(outstanding)
                } else {
                    // No available-limit in SMS and no user limit set — fall back to FIFO ledger
                    fulizaLoanRepository.recordRepayment(
                        drawCode = parsed.mpesaCode,
                        repaidAmountKes = parsed.amount,
                        repaymentDate = parsed.date,
                    )
                }
            }

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
    IMPORTED_REALTIME,
    IMPORTED_BATCH_PENDING,
    IMPORTED_QUARANTINE,

    // Fuliza charge notice — outstanding balance updated, no transaction imported
    FULIZA_BALANCE_UPDATED,
}
