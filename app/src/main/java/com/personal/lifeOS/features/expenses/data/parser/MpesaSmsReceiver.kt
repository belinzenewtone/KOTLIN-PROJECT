package com.personal.lifeOS.features.expenses.data.parser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Receives incoming SMS messages in real-time and checks for MPESA transactions.
 * Parsed transactions are saved to the local database.
 */
class MpesaSmsReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val fullMessage =
            messages
                .mapNotNull { it.messageBody }
                .joinToString("")

        if (!MpesaSmsParser.isMpesaSms(fullMessage)) return

        val parsed = MpesaSmsParser.parse(fullMessage) ?: return
        val request =
            OneTimeWorkRequestBuilder<MpesaSmsWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(MpesaSmsWorker.KEY_SMS_BODY, fullMessage)
                        .build(),
                )
                .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "mpesa-sms-${parsed.mpesaCode}",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
