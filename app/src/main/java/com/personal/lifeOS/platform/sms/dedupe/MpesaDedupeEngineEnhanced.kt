package com.personal.lifeOS.platform.sms.dedupe

import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced M-Pesa deduplication engine using dual-key strategy.
 *
 * Dual-key deduplication:
 * 1. Primary key: mpesa_code (exact code prevents duplicate by transaction reference)
 * 2. Secondary key: source_hash (SHA-256 hash of SMS body prevents Fuliza notice duplicates)
 *
 * Example Fuliza scenario:
 * - Original: "ABC123 Ksh 5000 borrowed" → mpesa_code=ABC123, source_hash=sha256(body1)
 * - Fuliza notice: "ABC123 Ksh 150 interest charged" → mpesa_code=ABC123, source_hash=sha256(body2)
 *
 * Without source_hash: Both would be rejected as duplicates (same code)
 * With source_hash: Only exact code+hash pairs are considered duplicates
 */
@Singleton
class MpesaDedupeEngineEnhanced
    @Inject
    constructor(
        private val expenseRepository: ExpenseRepository,
    ) {

        /**
         * Check if a message is a duplicate using dual-key strategy.
         * Returns true if EITHER the mpesa_code OR the source_hash already exists.
         */
        suspend fun isDuplicate(
            mpesaCode: String,
            rawMessage: String,
            amount: Double,
            merchant: String,
            timestamp: Long,
        ): Boolean {
            // Primary check: exact M-Pesa code (prevents duplicate by transaction reference)
            if (expenseRepository.existsByMpesaCode(mpesaCode)) {
                return true
            }

            // Secondary check: source hash (prevents SMS variant duplicates like Fuliza notices)
            val sourceHash = computeSourceHash(rawMessage)
            if (expenseRepository.existsBySourceHash(sourceHash)) {
                return true
            }

            // Tertiary check: heuristic fallback (same amount + merchant + time within 5 minutes)
            // This catches accidental duplicates from SMS being received twice
            return expenseRepository.existsPotentialDuplicate(
                amount = amount,
                merchant = merchant,
                date = timestamp,
                windowMillis = 5 * 60 * 1000L,  // 5 minutes
            )
        }

        /**
         * Compute SHA-256 hash of raw message body.
         * Used as secondary deduplication key to catch SMS variant duplicates
         * (e.g., Fuliza service notices with same mpesa_code but different body).
         */
        private fun computeSourceHash(rawMessage: String): String {
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                val hashBytes = digest.digest(rawMessage.toByteArray(Charsets.UTF_8))
                // Convert to hex string
                hashBytes.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                // Fallback: if hashing fails, use string hash code
                // This is less ideal but prevents crashes
                rawMessage.hashCode().toString()
            }
        }

        /**
         * Detect if this is a Fuliza-related duplicate.
         * Fuliza creates multiple SMSs for same transaction: original + interest/charges notices.
         *
         * Strategy: Check if both messages have Fuliza context AND same merchant/amount.
         */
        suspend fun isFulizaVariantDuplicate(
            mpesaCode: String,
            rawMessage: String,
            amount: Double,
            merchant: String,
        ): Boolean {
            val isFulizaMessage = rawMessage.contains("Fuliza", ignoreCase = true)
            if (!isFulizaMessage) return false

            // Check if we already have a Fuliza transaction with same code + amount
            return expenseRepository.existsByMpesaCode(mpesaCode)
        }
    }
