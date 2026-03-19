package com.personal.lifeOS.core.notifications

import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.dao.TaskDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderSettingsCoordinator
    @Inject
    constructor(
        private val eventDao: EventDao,
        private val taskDao: TaskDao,
        private val eventReminderScheduler: EventReminderScheduler,
        private val taskReminderScheduler: TaskReminderScheduler,
    ) {
        suspend fun cancelAllScheduledRemindersForUser(userId: String): Int {
            if (userId.isBlank()) return 0

            val eventIds = eventDao.getScheduledReminderIds(userId)
            val taskIds = taskDao.getScheduledReminderIds(userId)

            eventIds.forEach { eventReminderScheduler.cancelEventReminder(it, userId) }
            taskIds.forEach { taskReminderScheduler.cancelTaskReminder(it, userId) }

            return eventIds.size + taskIds.size
        }
    }
