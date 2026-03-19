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
        override suspend fun ingestRealtime(rawMessage: String): Boolean {
            if (!parser.isMpesaMessage(rawMessage)) {
                importAuditLogger.log(
                    outcome = "ignored_irrelevant",
                    rawMessage = rawMessage,
                )
                return false
            }

            val parsed = parser.parse(rawMessage)
            if (parsed == null) {
                importAuditLogger.log(
                    outcome = "parse_failed",
                    rawMessage = rawMessage,
                    failureReason = "Parser returned null",
                )
                return false
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
                return false
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
                return false
            }

            importAuditLogger.log(
                outcome = "imported",
                rawMessage = rawMessage,
                mpesaCode = parsed.mpesaCode,
                amount = parsed.amount,
                merchant = parsed.merchant,
            )
            return true
        }
    }
