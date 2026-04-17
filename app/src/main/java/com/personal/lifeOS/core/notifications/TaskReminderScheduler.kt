package com.personal.lifeOS.core.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.personal.lifeOS.R
import com.personal.lifeOS.core.preferences.AppSettingsStore
import com.personal.lifeOS.features.tasks.domain.model.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskReminderScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val appSettingsStore: AppSettingsStore,
    ) {
        suspend fun scheduleTaskReminder(
            task: Task,
            userId: String,
        ): Boolean {
            val deadline = task.deadline ?: return false
            if (task.id <= 0L) return false
            if (!appSettingsStore.areNotificationsEnabled()) {
                cancelTaskReminder(task.id, userId)
                return false
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
            val triggers = computeReminderTriggers(deadline, task.reminderOffsets, System.currentTimeMillis())
            cancelTaskReminder(task.id, userId)
            if (triggers.isEmpty()) return false

            triggers.forEachIndexed { index, triggerAt ->
                val pendingIntent =
                    buildReminderPendingIntent(
                        context = context,
                        taskId = task.id,
                        userId = userId,
                        offsetIndex = index,
                        title = task.title,
                        description = task.description,
                    )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
            }
            return true
        }

        fun cancelTaskReminder(
            taskId: Long,
            userId: String,
            offsetCount: Int = MAX_REMINDER_OFFSETS,
        ) {
            if (taskId <= 0L) return
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            repeat(offsetCount) { index ->
                val pendingIntent =
                    buildReminderPendingIntent(
                        context = context,
                        taskId = taskId,
                        userId = userId,
                        offsetIndex = index,
                        title = "",
                        description = "",
                    )
                alarmManager.cancel(pendingIntent)
            }
        }

        companion object {
            const val CHANNEL_ID = "task_reminders"
            const val EXTRA_TASK_ID = "extra_task_id"
            const val EXTRA_USER_ID = "extra_user_id"
            const val EXTRA_TITLE = "extra_title"
            const val EXTRA_DESCRIPTION = "extra_description"
            const val DEFAULT_REMINDER_OFFSET_MS = 30L * 60L * 1000L
            const val MAX_REMINDER_OFFSETS = 10

            @VisibleForTesting
            internal fun computeReminderTriggers(
                deadline: Long,
                reminderOffsets: List<Int>,
                nowMs: Long,
            ): List<Long> {
                return reminderOffsets
                    .asSequence()
                    .map { it.coerceAtLeast(0) }
                    .distinct()
                    .map { minutes -> deadline - minutes * 60_000L }
                    .filter { triggerAt -> triggerAt > nowMs }
                    .sorted()
                    .toList()
            }

            fun ensureNotificationChannel(context: Context) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
                val channel =
                    NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.task_reminder_channel_name),
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = context.getString(R.string.task_reminder_channel_description)
                    }
                manager.createNotificationChannel(channel)
            }

            private fun buildReminderPendingIntent(
                context: Context,
                taskId: Long,
                userId: String,
                offsetIndex: Int = 0,
                title: String,
                description: String,
            ): PendingIntent {
                val requestCode = reminderRequestCode(userId = userId, taskId = taskId, offsetIndex = offsetIndex)
                val intent =
                    Intent(context, TaskReminderReceiver::class.java).apply {
                        putExtra(EXTRA_TASK_ID, taskId)
                        putExtra(EXTRA_USER_ID, userId)
                        putExtra(EXTRA_TITLE, title)
                        putExtra(EXTRA_DESCRIPTION, description)
                    }
                return PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }

            @VisibleForTesting
            internal fun reminderRequestCode(
                userId: String,
                taskId: Long,
                offsetIndex: Int = 0,
            ): Int {
                return "$userId:$taskId:$offsetIndex".hashCode()
            }
        }
    }
