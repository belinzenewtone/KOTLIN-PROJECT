package com.personal.lifeOS.platform.sms.background

import android.content.Context
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager task that scans the device SMS inbox every 30 minutes
 * for M-Pesa messages that may have been missed by the realtime BroadcastReceiver
 * (e.g. while the app was force-stopped or the receiver was unregistered).
 *
 * Behaviour:
 *  - First run: scans the last 90 days of SMS history (initial backfill window)
 *  - Subsequent runs: scans from (lastSyncTimestamp - 2 minutes) to now, using
 *    a 2-minute overlap to catch any message that arrived mid-previous-sync
 *  - Deduplication in [MpesaDedupeEngineEnhanced] prevents double-import
 */
class MpesaPeriodicSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val historicalScanner = historicalScanner()
            // Determine how far back to scan
            val lastSyncMs = loadLastSyncTimestamp()
            val daysBack = if (lastSyncMs == null) {
                INITIAL_BACKFILL_DAYS
            } else {
                // scan 2 extra minutes beyond last sync to avoid gaps
                val elapsedMs = System.currentTimeMillis() - lastSyncMs + OVERLAP_MS
                val elapsedDays = (elapsedMs / MS_PER_DAY).toInt().coerceAtLeast(1)
                elapsedDays
            }

            historicalScanner.scan(daysBack)
            saveLastSyncTimestamp(System.currentTimeMillis())
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun loadLastSyncTimestamp(): Long? {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getLong(KEY_LAST_SYNC, -1L)
        return if (value == -1L) null else value
    }

    private suspend fun saveLastSyncTimestamp(timestamp: Long) {
        applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putLong(KEY_LAST_SYNC, timestamp)
            }
    }

    private fun historicalScanner(): MpesaHistoricalImportScanner {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            MpesaPeriodicSyncEntryPoint::class.java,
        )
        return entryPoint.historicalScanner()
    }

    companion object {
        private const val WORK_NAME       = "mpesa-periodic-sms-sync"
        private const val PREFS_NAME      = "mpesa_periodic_sync_prefs"
        private const val KEY_LAST_SYNC   = "last_sync_timestamp_ms"
        private const val INITIAL_BACKFILL_DAYS = 90
        private const val OVERLAP_MS      = 2 * 60 * 1000L   // 2-minute overlap
        private const val MS_PER_DAY      = 86_400_000L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MpesaPeriodicSyncWorker>(
                30, TimeUnit.MINUTES,
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,  // don't reset if already queued
                request,
            )
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MpesaPeriodicSyncEntryPoint {
    fun historicalScanner(): MpesaHistoricalImportScanner
}
