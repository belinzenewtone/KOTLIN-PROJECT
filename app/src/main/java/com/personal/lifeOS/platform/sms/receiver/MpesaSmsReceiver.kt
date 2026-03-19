package com.personal.lifeOS.platform.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.personal.lifeOS.platform.sms.background.MpesaSmsIngestionWorker
import com.personal.lifeOS.platform.sms.parser.MpesaMessageParser
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MpesaSmsReceiver : BroadcastReceiver() {
    @Inject
    lateinit var parser: MpesaMessageParser

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val fullMessage =
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
                .mapNotNull { it.messageBody }
                .joinToString("")

        if (!parser.isMpesaMessage(fullMessage)) return
        val parsed = parser.parse(fullMessage) ?: return

        val request =
            OneTimeWorkRequestBuilder<MpesaSmsIngestionWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(MpesaSmsIngestionWorker.KEY_SMS_BODY, fullMessage)
                        .build(),
                )
                .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "mpesa-platform-${parsed.mpesaCode}",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
