package com.personal.lifeOS

import android.app.Application
import com.personal.lifeOS.core.notifications.EventReminderScheduler
import com.personal.lifeOS.core.notifications.TaskReminderScheduler
import com.personal.lifeOS.core.work.CloudSyncScheduler
import com.personal.lifeOS.core.work.RecurringExecutionScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LifeOSApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        EventReminderScheduler.ensureNotificationChannel(this)
        TaskReminderScheduler.ensureNotificationChannel(this)
        CloudSyncScheduler.schedulePeriodic(this)
        RecurringExecutionScheduler.schedulePeriodic(this)
    }
}
