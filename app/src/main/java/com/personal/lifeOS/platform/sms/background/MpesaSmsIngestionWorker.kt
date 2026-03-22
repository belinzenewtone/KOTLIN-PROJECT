package com.personal.lifeOS.platform.sms.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.personal.lifeOS.platform.sms.ingestion.MpesaIngestionPipeline
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class MpesaSmsIngestionWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val smsBody = inputData.getString(KEY_SMS_BODY).orEmpty()
        if (smsBody.isBlank()) return Result.failure()

        return try {
            // Queue size cap: prevent runaway queue growth during extended outages
            val syncJobDao = entryPointAccessor().syncJobDao()
            val pendingCount = syncJobDao.getPendingCount()
            if (pendingCount >= 400) {
                // Queue is full — skip import this cycle, only flush cloud sync
                return Result.success()
            }

            ingestionPipeline().ingestRealtime(smsBody)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun entryPointAccessor(): MpesaSmsIngestionEntryPoint {
        return EntryPointAccessors.fromApplication(
            applicationContext,
            MpesaSmsIngestionEntryPoint::class.java,
        )
    }

    private fun ingestionPipeline(): MpesaIngestionPipeline {
        val entryPoint =
            EntryPointAccessors.fromApplication(
                applicationContext,
                MpesaSmsIngestionEntryPoint::class.java,
            )
        return entryPoint.pipeline()
    }

    companion object {
        const val KEY_SMS_BODY = "sms_body"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MpesaSmsIngestionEntryPoint {
    fun pipeline(): MpesaIngestionPipeline
    fun syncJobDao(): com.personal.lifeOS.core.database.dao.SyncJobDao
}
