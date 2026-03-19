package com.personal.lifeOS.core.notifications

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.personal.lifeOS.R
import com.personal.lifeOS.core.preferences.AppSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
                if (!notificationsEnabled) return@launch
                if (!AppSettingsStore.areNotificationsEnabled(context)) return@launch

                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@launch
                }

                val taskId = intent.getLongExtra(TaskReminderScheduler.EXTRA_TASK_ID, -1L)
                val title =
                    intent.getStringExtra(TaskReminderScheduler.EXTRA_TITLE)
                        ?.takeIf { it.isNotBlank() }
                        ?: context.getString(R.string.task_reminder_default_title)
                val description =
                    intent.getStringExtra(TaskReminderScheduler.EXTRA_DESCRIPTION)
                        ?.takeIf { it.isNotBlank() }
                        ?: context.getString(R.string.task_reminder_default_body)

                val notification =
                    NotificationCompat.Builder(context, TaskReminderScheduler.CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(description)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build()

                val notificationId = if (taskId > 0L) taskId.hashCode() else System.currentTimeMillis().toInt()
                NotificationManagerCompat.from(context).notify(notificationId, notification)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
