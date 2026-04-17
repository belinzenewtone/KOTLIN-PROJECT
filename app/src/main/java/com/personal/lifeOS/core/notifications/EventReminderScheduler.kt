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

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
            val now = System.currentTimeMillis()
            // Use reminderOffsets when available, fall back to reminderMinutesBefore
            val offsets = event.reminderOffsets.ifEmpty { listOf(event.reminderMinutesBefore) }
            var scheduled = false
            val timeOfDayMs = event.reminderTimeOfDayMinutes.coerceIn(0, 1439) * 60_000L
            offsets.forEachIndexed { index, minutes ->
                val triggerAt = if (event.allDay) {
                    // For all-day events, offset is days-before; fire at user's chosen time-of-day.
                    event.date - (minutes.coerceAtLeast(0) * 60_000L) + timeOfDayMs
                } else {
                    event.date - (minutes.coerceAtLeast(0) * 60_000L)
                }
                if (triggerAt <= now) return@forEachIndexed
                val pendingIntent = buildReminderPendingIntent(
                    context = context,
                    eventId = event.id,
                    userId = userId,
                    offsetIndex = index,
                    title = event.title,
                    description = event.description,
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
                scheduled = true
            }
            return scheduled
        }

        fun cancelEventReminder(
            eventId: Long,
            userId: String,
            offsetCount: Int = MAX_REMINDER_OFFSETS,
        ) {
            if (eventId <= 0L) return
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            repeat(offsetCount) { index ->
                val pendingIntent = buildReminderPendingIntent(
                    context = context,
                    eventId = eventId,
                    userId = userId,
                    offsetIndex = index,
                    title = "",
                    description = "",
                )
                alarmManager.cancel(pendingIntent)
            }
        }

        companion object {
            const val CHANNEL_ID = "event_reminders"
            const val EXTRA_EVENT_ID = "extra_event_id"
            const val EXTRA_USER_ID = "extra_user_id"
            const val EXTRA_TITLE = "extra_title"
            const val EXTRA_DESCRIPTION = "extra_description"
            // Upper bound used when cancelling all alarms for an event without knowing the count
            const val MAX_REMINDER_OFFSETS = 10

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
                offsetIndex: Int,
                title: String,
                description: String,
            ): PendingIntent {
                val requestCode = reminderRequestCode(userId = userId, eventId = eventId, offsetIndex = offsetIndex)
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
                offsetIndex: Int = 0,
            ): Int {
                return "$userId:$eventId:$offsetIndex".hashCode()
            }
        }
    }
