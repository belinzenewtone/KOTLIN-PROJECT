package com.personal.lifeOS.platform.sms.audit

import com.personal.lifeOS.core.database.LocalIdGenerator
import com.personal.lifeOS.core.database.dao.ImportAuditDao
import com.personal.lifeOS.core.database.entity.ImportAuditEntity
import com.personal.lifeOS.core.observability.AppTelemetry
import com.personal.lifeOS.core.security.AuthSessionStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportAuditLogger
    @Inject
    constructor(
        private val importAuditDao: ImportAuditDao,
        private val authSessionStore: AuthSessionStore,
    ) {
        suspend fun log(
            outcome: String,
            rawMessage: String,
            mpesaCode: String? = null,
            amount: Double? = null,
            merchant: String? = null,
            failureReason: String? = null,
        ) {
            publishTelemetry(
                outcome = outcome,
                mpesaCode = mpesaCode,
                amount = amount,
                merchant = merchant,
            )
            val userId = authSessionStore.getUserId()
            if (userId.isBlank()) return
            importAuditDao.insert(
                ImportAuditEntity(
                    id = LocalIdGenerator.nextId(),
                    userId = userId,
                    rawMessage = rawMessage,
                    mpesaCode = mpesaCode,
                    amount = amount,
                    merchant = merchant,
                    outcome = outcome,
                    failureReason = failureReason,
                ),
            )
        }

        private fun publishTelemetry(
            outcome: String,
            mpesaCode: String?,
            amount: Double?,
            merchant: String?,
        ) {
            val safeOutcome = outcome.lowercase()
            AppTelemetry.trackEvent(
                name = "parser_result",
                attributes =
                    mapOf(
                        "outcome" to safeOutcome,
                        "has_mpesa_code" to (!mpesaCode.isNullOrBlank()).toString(),
                        "amount_band" to amountBand(amount),
                        "has_merchant" to (!merchant.isNullOrBlank()).toString(),
                    ),
            )

            if (safeOutcome.contains("duplicate")) {
                AppTelemetry.trackEvent(
                    name = "dedupe_hit",
                    attributes = mapOf("source" to "sms_import", "outcome" to safeOutcome),
                )
            }

            if (
                safeOutcome.contains("candidate_pending") ||
                safeOutcome.contains("quarantine") ||
                safeOutcome.contains("parse_failed")
            ) {
                AppTelemetry.trackEvent(
                    name = "parser_quarantine",
                    attributes = mapOf("outcome" to safeOutcome),
                )
            }
        }

        private fun amountBand(amount: Double?): String {
            if (amount == null) return "unknown"
            return when {
                amount < 100.0 -> "lt_100"
                amount < 1000.0 -> "100_999"
                amount < 10000.0 -> "1000_9999"
                else -> "10000_plus"
            }
        }
    }
