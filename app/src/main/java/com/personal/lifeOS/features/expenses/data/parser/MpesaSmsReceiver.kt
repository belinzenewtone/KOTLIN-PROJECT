package com.personal.lifeOS.features.expenses.data.parser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import dagger.hilt.android.AndroidEntryPoint

/**
 * Receives incoming SMS messages in real-time and checks for MPESA transactions.
 * Parsed transactions are saved to the local database.
 */
@AndroidEntryPoint
class MpesaSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val fullMessage = messages
            .mapNotNull { it.messageBody }
            .joinToString("")

        if (!MpesaSmsParser.isMpesaSms(fullMessage)) return

        val parsed = MpesaSmsParser.parse(fullMessage) ?: return

        // TODO: Use WorkManager or a coroutine scope to save to database
        // For now, we'll broadcast an intent that the app can pick up
        val resultIntent = Intent("com.personal.lifeOS.MPESA_TRANSACTION").apply {
            putExtra("mpesa_code", parsed.mpesaCode)
            putExtra("amount", parsed.amount)
            putExtra("merchant", parsed.merchant)
            putExtra("type", parsed.transactionType.name)
            putExtra("date", parsed.date)
            putExtra("raw_sms", parsed.rawSms)
        }
        context.sendBroadcast(resultIntent)
    }
}
