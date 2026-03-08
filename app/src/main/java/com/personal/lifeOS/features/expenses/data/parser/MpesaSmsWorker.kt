package com.personal.lifeOS.features.expenses.data.parser

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class MpesaSmsWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val smsBody = inputData.getString(KEY_SMS_BODY).orEmpty()
        if (smsBody.isBlank()) return Result.failure()

        if (!MpesaSmsParser.isMpesaSms(smsBody)) return Result.success()

        return try {
            repository().importFromSms(smsBody)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun repository(): ExpenseRepository {
        val entryPoint =
            EntryPointAccessors.fromApplication(
                applicationContext,
                MpesaSmsWorkerEntryPoint::class.java,
            )
        return entryPoint.expenseRepository()
    }

    companion object {
        const val KEY_SMS_BODY = "sms_body"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MpesaSmsWorkerEntryPoint {
    fun expenseRepository(): ExpenseRepository
}
