package com.personal.lifeOS.platform.sms.background

import android.content.Context
import android.net.Uri
import com.personal.lifeOS.platform.sms.ingestion.MpesaIngestionOutcome
import com.personal.lifeOS.platform.sms.ingestion.MpesaIngestionPipeline
import com.personal.lifeOS.platform.sms.ingestion.MpesaIngestionSource
import com.personal.lifeOS.platform.sms.parser.MpesaMessageParser
import com.personal.lifeOS.platform.sms.permissions.SmsPermissionGateway
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class MpesaHistoricalImportSummary(
    val scannedMessages: Int = 0,
    val imported: Int = 0,
    val duplicates: Int = 0,
    val parseFailed: Int = 0,
    val pendingReview: Int = 0,
    val ignoredIrrelevant: Int = 0,
    val permissionGranted: Boolean = true,
)

@Singleton
class MpesaHistoricalImportScanner
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val permissionGateway: SmsPermissionGateway,
        private val parser: MpesaMessageParser,
        private val ingestionPipeline: MpesaIngestionPipeline,
    ) {
        suspend fun scan(daysBack: Int): MpesaHistoricalImportSummary =
            withContext(Dispatchers.IO) {
                if (!permissionGateway.hasReadSms()) {
                    return@withContext MpesaHistoricalImportSummary(permissionGranted = false)
                }

                val normalizedDaysBack = daysBack.coerceIn(1, 365)
                val cutoff = System.currentTimeMillis() - (normalizedDaysBack.toLong() * ONE_DAY_MILLIS)
                val cursor =
                    context.contentResolver.query(
                        Uri.parse("content://sms/inbox"),
                        arrayOf("body", "date", "address"),
                        "address LIKE ? AND date >= ?",
                        arrayOf("%MPESA%", cutoff.toString()),
                        "date DESC",
                    )

                var scannedMessages = 0
                var imported = 0
                var duplicates = 0
                var parseFailed = 0
                var pendingReview = 0
                var ignoredIrrelevant = 0

                cursor?.use { inbox ->
                    val bodyIndex = inbox.getColumnIndexOrThrow("body")
                    while (inbox.moveToNext()) {
                        val rawBody = inbox.getString(bodyIndex).orEmpty()
                        if (rawBody.isBlank()) {
                            // Ignore empty bodies from inbox rows.
                        } else if (!parser.isMpesaMessage(rawBody)) {
                            ignoredIrrelevant += 1
                        } else {
                            scannedMessages += 1

                            when (ingestionPipeline.ingestRealtime(rawBody, MpesaIngestionSource.BACKFILL)) {
                                MpesaIngestionOutcome.IMPORTED -> imported += 1
                                MpesaIngestionOutcome.DUPLICATE -> duplicates += 1
                                MpesaIngestionOutcome.PARSE_FAILED -> parseFailed += 1
                                MpesaIngestionOutcome.CANDIDATE_PENDING -> pendingReview += 1
                                MpesaIngestionOutcome.IGNORED_IRRELEVANT -> ignoredIrrelevant += 1
                            }
                        }
                    }
                }

                MpesaHistoricalImportSummary(
                    scannedMessages = scannedMessages,
                    imported = imported,
                    duplicates = duplicates,
                    parseFailed = parseFailed,
                    pendingReview = pendingReview,
                    ignoredIrrelevant = ignoredIrrelevant,
                    permissionGranted = true,
                )
            }

        private companion object {
            const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L
        }
    }
