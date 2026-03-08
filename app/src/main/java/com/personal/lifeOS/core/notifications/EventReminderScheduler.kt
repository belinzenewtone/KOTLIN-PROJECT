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
import com.personal.lifeOS.features.calendar.domain.model.CalendarEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventReminderScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val appSettingsStore: AppSettingsStore,
    ) {
        suspend fun scheduleEventReminder(
            event: CalendarEvent,
            userId: String,
        ): Boolean {
            if (!event.hasReminder || event.id <= 0L) return false
            if (!appSettingsStore.areNotificationsEnabled()) {
                cancelEventReminder(event.id, userId)
                return false
            }

            val triggerAt = event.date - (event.reminderMinutesBefore.coerceAtLeast(0) * 60_000L)
            if (triggerAt <= System.currentTimeMillis()) return false

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
            val pendingIntent =
                buildReminderPendingIntent(
                    context = context,
                    eventId = event.id,
                    userId = userId,
                    title = event.title,
                    description = event.description,
                )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                return true
            }

            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            return true
        }

        fun cancelEventReminder(
            eventId: Long,
            userId: String,
        ) {
            if (eventId <= 0L) return
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pendingIntent =
                buildReminderPendingIntent(
                    context = context,
                    eventId = eventId,
                    userId = userId,
                    title = "",
                    description = "",
                )
            alarmManager.cancel(pendingIntent)
        }

        companion object {
            const val CHANNEL_ID = "event_reminders"
            const val EXTRA_EVENT_ID = "extra_event_id"
            const val EXTRA_USER_ID = "extra_user_id"
            const val EXTRA_TITLE = "extra_title"
            const val EXTRA_DESCRIPTION = "extra_description"

            fun ensureNotificationChannel(context: Context) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
                val channel =
                    NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.event_reminder_channel_name),
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = context.getString(R.string.event_reminder_channel_description)
                    }
                manager.createNotificationChannel(channel)
            }

            private fun buildReminderPendingIntent(
                context: Context,
                eventId: Long,
                userId: String,
                title: String,
                description: String,
            ): PendingIntent {
                val requestCode = reminderRequestCode(userId = userId, eventId = eventId)
                val intent =
                    Intent(context, EventReminderReceiver::class.java).apply {
                        putExtra(EXTRA_EVENT_ID, eventId)
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
                eventId: Long,
            ): Int {
                return "$userId:$eventId".hashCode()
            }
        }
    }
