package com.personal.lifeOS.features.expenses.data.datasource

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import com.personal.lifeOS.features.expenses.data.parser.MpesaSmsParser
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans existing SMS inbox for MPESA messages and imports them.
 * Used on first launch or manual sync to backfill transaction history.
 */
@Singleton
class SmsImportService
    @Inject
    constructor(
        private val repository: ExpenseRepository,
    ) {
        /**
         * Scan SMS inbox and import all MPESA transactions.
         * Skips duplicates automatically (by mpesa code).
         *
         * @return number of new transactions imported
         */
        suspend fun importFromInbox(contentResolver: ContentResolver): Int =
            withContext(Dispatchers.IO) {
                var imported = 0
                var cursor: Cursor? = null

                try {
                    cursor =
                        contentResolver.query(
                            Uri.parse("content://sms/inbox"),
                            arrayOf("_id", "address", "body", "date"),
                            "address LIKE ?",
                            arrayOf("%MPESA%"),
                            "date DESC",
                        )

                    cursor?.let {
                        val bodyIndex = it.getColumnIndexOrThrow("body")

                        while (it.moveToNext()) {
                            val body = it.getString(bodyIndex) ?: continue

                            if (!MpesaSmsParser.isMpesaSms(body)) continue

                            val result = repository.importFromSms(body)
                            if (result != null) imported++
                        }
                    }
                } catch (e: Exception) {
                    // Fault resistance: log but don't crash
                    e.printStackTrace()
                } finally {
                    cursor?.close()
                }

                imported
            }
    }
