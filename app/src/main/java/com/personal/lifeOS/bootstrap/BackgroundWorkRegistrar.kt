package com.personal.lifeOS.bootstrap

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.personal.lifeOS.core.work.CloudSyncScheduler
import com.personal.lifeOS.core.work.PruneImportAuditWorker
import com.personal.lifeOS.core.work.RecurringExecutionScheduler
import com.personal.lifeOS.platform.sms.background.MpesaPeriodicSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundWorkRegistrar
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun registerAll() {
            CloudSyncScheduler.schedulePeriodic(context)
            RecurringExecutionScheduler.schedulePeriodic(context)
            MpesaPeriodicSyncWorker.schedule(context)
            schedulePruneImportAudit()
        }

        private fun schedulePruneImportAudit() {
            // Weekly import audit pruning
            val pruneRequest = PeriodicWorkRequestBuilder<PruneImportAuditWorker>(7, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "prune_import_audit",
                ExistingPeriodicWorkPolicy.KEEP,
                pruneRequest,
            )
        }
    }
