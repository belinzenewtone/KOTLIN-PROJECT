package com.personal.lifeOS.platform.sms.ingestion

import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import com.personal.lifeOS.platform.sms.audit.ImportAuditLogger
import com.personal.lifeOS.platform.sms.dedupe.MpesaDedupeEngine
import com.personal.lifeOS.platform.sms.parser.MpesaMessageParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultMpesaIngestionPipeline
    @Inject
    constructor(
        private val parser: MpesaMessageParser,
        private val dedupeEngine: MpesaDedupeEngine,
        private val expenseRepository: ExpenseRepository,
        private val importAuditLogger: ImportAuditLogger,
    ) : MpesaIngestionPipeline {
        override suspend fun ingestRealtime(
            rawMessage: String,
            source: MpesaIngestionSource,
        ): MpesaIngestionOutcome {
            if (!parser.isMpesaMessage(rawMessage)) {
                importAuditLogger.log(
                    outcome = "ignored_irrelevant",
                    rawMessage = rawMessage,
                )
                return MpesaIngestionOutcome.IGNORED_IRRELEVANT
            }

            val parsed = parser.parse(rawMessage)
            if (parsed == null) {
                importAuditLogger.log(
                    outcome = "parse_failed",
                    rawMessage = rawMessage,
                    failureReason = "Parser returned null",
                )
                return MpesaIngestionOutcome.PARSE_FAILED
            }

            if (
                dedupeEngine.isDuplicate(
                    mpesaCode = parsed.mpesaCode,
                    amount = parsed.amount,
                    merchant = parsed.merchant,
                    timestamp = parsed.date,
                )
            ) {
                importAuditLogger.log(
                    outcome = "duplicate",
                    rawMessage = rawMessage,
                    mpesaCode = parsed.mpesaCode,
                    amount = parsed.amount,
                    merchant = parsed.merchant,
                )
                return MpesaIngestionOutcome.DUPLICATE
            }

            val created = expenseRepository.importFromSms(rawMessage)
            if (created == null) {
                importAuditLogger.log(
                    outcome = "candidate_pending",
                    rawMessage = rawMessage,
                    mpesaCode = parsed.mpesaCode,
                    amount = parsed.amount,
                    merchant = parsed.merchant,
                    failureReason = "Repository import returned null",
                )
                return MpesaIngestionOutcome.CANDIDATE_PENDING
            }

            importAuditLogger.log(
                outcome =
                    if (source == MpesaIngestionSource.BACKFILL) {
                        "recovered_from_backfill"
                    } else {
                        "imported"
                    },
                rawMessage = rawMessage,
                mpesaCode = parsed.mpesaCode,
                amount = parsed.amount,
                merchant = parsed.merchant,
            )
            return MpesaIngestionOutcome.IMPORTED
        }
    }
