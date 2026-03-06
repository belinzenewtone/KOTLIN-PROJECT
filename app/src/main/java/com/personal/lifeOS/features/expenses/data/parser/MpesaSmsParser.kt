package com.personal.lifeOS.features.expenses.data.parser

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Parses MPESA SMS messages and extracts structured transaction data.
 *
 * Handles the following MPESA transaction types:
 * - Sent (to person or till)
 * - Received
 * - Paid (Lipa Na MPESA)
 * - Withdrawn
 * - Bought airtime
 */
object MpesaSmsParser {

    data class ParsedTransaction(
        val mpesaCode: String,
        val amount: Double,
        val merchant: String,
        val transactionType: TransactionType,
        val date: Long,
        val rawSms: String
    )

    enum class TransactionType {
        SENT, RECEIVED, PAID, WITHDRAWN, AIRTIME, DEPOSIT, UNKNOWN
    }

    private val MPESA_CODE_REGEX = Regex("[A-Z0-9]{10}")
    private val AMOUNT_REGEX = Regex("Ksh([\\d,]+\\.?\\d*)")
    private val DATE_REGEX = Regex("on (\\d{1,2}/\\d{1,2}/\\d{2,4})")
    private val TIME_REGEX = Regex("at (\\d{1,2}:\\d{2} [AP]M)")

    /**
     * Check if an SMS is an MPESA message.
     */
    fun isMpesaSms(message: String): Boolean {
        val upperMsg = message.uppercase()
        return upperMsg.contains("MPESA") ||
                upperMsg.contains("M-PESA") ||
                (MPESA_CODE_REGEX.containsMatchIn(message) && AMOUNT_REGEX.containsMatchIn(message))
    }

    /**
     * Parse an MPESA SMS and return structured transaction data.
     * Returns null if parsing fails.
     */
    fun parse(sms: String): ParsedTransaction? {
        return try {
            val code = extractMpesaCode(sms) ?: return null
            val amount = extractAmount(sms) ?: return null
            val type = detectTransactionType(sms)
            val merchant = extractMerchant(sms, type)
            val date = extractDate(sms)

            ParsedTransaction(
                mpesaCode = code,
                amount = amount,
                merchant = merchant,
                transactionType = type,
                date = date,
                rawSms = sms
            )
        } catch (e: Exception) {
            // Fault resistance: ignore malformed messages
            null
        }
    }

    private fun extractMpesaCode(sms: String): String? {
        return MPESA_CODE_REGEX.find(sms)?.value
    }

    private fun extractAmount(sms: String): Double? {
        val match = AMOUNT_REGEX.find(sms) ?: return null
        val amountStr = match.groupValues[1].replace(",", "")
        return amountStr.toDoubleOrNull()
    }

    private fun detectTransactionType(sms: String): TransactionType {
        val lower = sms.lowercase()
        return when {
            lower.contains("sent to") -> TransactionType.SENT
            lower.contains("received") -> TransactionType.RECEIVED
            lower.contains("paid to") || lower.contains("buy goods") -> TransactionType.PAID
            lower.contains("withdraw") -> TransactionType.WITHDRAWN
            lower.contains("airtime") -> TransactionType.AIRTIME
            lower.contains("deposit") -> TransactionType.DEPOSIT
            else -> TransactionType.UNKNOWN
        }
    }

    private fun extractMerchant(sms: String, type: TransactionType): String {
        return try {
            when (type) {
                TransactionType.SENT -> {
                    // "sent to JOHN DOE" or "sent to 0712345678"
                    val regex = Regex("sent to ([A-Z\\s]+?)(?:\\s+\\d|\\.|on )", RegexOption.IGNORE_CASE)
                    regex.find(sms)?.groupValues?.get(1)?.trim() ?: "Unknown"
                }
                TransactionType.PAID -> {
                    // "paid to KFC WESTLANDS" or "Buy Goods from NAIVAS"
                    val regex = Regex("(?:paid to|from)\\s+([A-Z0-9\\s&'-]+?)(?:\\s+on |\\.|New)", RegexOption.IGNORE_CASE)
                    regex.find(sms)?.groupValues?.get(1)?.trim() ?: "Unknown"
                }
                TransactionType.RECEIVED -> {
                    val regex = Regex("from ([A-Z\\s]+?)(?:\\s+\\d|\\.|on )", RegexOption.IGNORE_CASE)
                    regex.find(sms)?.groupValues?.get(1)?.trim() ?: "Unknown"
                }
                TransactionType.WITHDRAWN -> "ATM Withdrawal"
                TransactionType.AIRTIME -> "Airtime"
                TransactionType.DEPOSIT -> "Deposit"
                TransactionType.UNKNOWN -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun extractDate(sms: String): Long {
        return try {
            val dateMatch = DATE_REGEX.find(sms)?.groupValues?.get(1) ?: return System.currentTimeMillis()
            val timeMatch = TIME_REGEX.find(sms)?.groupValues?.get(1) ?: "12:00 PM"
            val dateTimeStr = "$dateMatch $timeMatch"
            val format = SimpleDateFormat("d/M/yy h:mm a", Locale.ENGLISH)
            format.parse(dateTimeStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
