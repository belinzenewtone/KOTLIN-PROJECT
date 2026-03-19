package com.personal.lifeOS.bootstrap

import android.content.Context
import com.personal.lifeOS.core.notifications.EventReminderScheduler
import com.personal.lifeOS.core.notifications.TaskReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationBootstrapper
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun ensureChannels() {
            EventReminderScheduler.ensureNotificationChannel(context)
            TaskReminderScheduler.ensureNotificationChannel(context)
        }
    }
