package com.personal.lifeOS.core.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.personal.lifeOS.features.recurring.data.RecurringExecutionService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class RecurringExecutionWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return runCatching {
            recurringExecutionService().processDueRules()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }

    private fun recurringExecutionService(): RecurringExecutionService {
        val entryPoint =
            EntryPointAccessors.fromApplication(
                applicationContext,
                RecurringExecutionWorkerEntryPoint::class.java,
            )
        return entryPoint.recurringExecutionService()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RecurringExecutionWorkerEntryPoint {
    fun recurringExecutionService(): RecurringExecutionService
}
