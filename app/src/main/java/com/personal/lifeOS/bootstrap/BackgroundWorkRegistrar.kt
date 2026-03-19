package com.personal.lifeOS.bootstrap

import android.content.Context
import com.personal.lifeOS.core.work.CloudSyncScheduler
import com.personal.lifeOS.core.work.RecurringExecutionScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
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
        }
    }
