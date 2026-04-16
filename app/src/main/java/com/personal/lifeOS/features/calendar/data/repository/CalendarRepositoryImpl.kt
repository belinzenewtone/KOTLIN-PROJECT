package com.personal.lifeOS.features.calendar.data.repository

import com.personal.lifeOS.core.database.LocalIdGenerator
import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.entity.EventEntity
import com.personal.lifeOS.core.notifications.EventReminderScheduler
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.core.sync.SyncMutationEnqueuer
import com.personal.lifeOS.features.calendar.domain.model.CalendarEvent
import com.personal.lifeOS.features.calendar.domain.model.EventImportance
import com.personal.lifeOS.features.calendar.domain.model.EventKind
import com.personal.lifeOS.features.calendar.domain.model.EventStatus
import com.personal.lifeOS.features.calendar.domain.model.EventType
import com.personal.lifeOS.features.calendar.domain.model.RepeatRule
import com.personal.lifeOS.features.calendar.domain.repository.CalendarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepositoryImpl
    @Inject
    constructor(
        private val eventDao: EventDao,
        private val authSessionStore: AuthSessionStore,
        private val reminderScheduler: EventReminderScheduler,
        private val syncMutationEnqueuer: SyncMutationEnqueuer,
    ) : CalendarRepository {
        private fun activeUserId(): String = authSessionStore.getUserId()

        override fun getEventsBetween(
            start: Long,
            end: Long,
        ): Flow<List<CalendarEvent>> {
            return eventDao.getEventsBetween(start, end, activeUserId()).map { it.map { e -> e.toDomain() } }
        }

        override fun getUpcomingEvents(limit: Int): Flow<List<CalendarEvent>> {
            return eventDao.getUpcomingEvents(System.currentTimeMillis(), activeUserId(), limit)
                .map { it.map { e -> e.toDomain() } }
        }

        override fun getByType(type: String): Flow<List<CalendarEvent>> {
            return eventDao.getByType(type, activeUserId()).map { it.map { e -> e.toDomain() } }
        }

        override suspend fun addEvent(event: CalendarEvent): Long {
            val userId = activeUserId()
            val stableId = if (event.id > 0L) event.id else LocalIdGenerator.nextId()
            val storedEvent = event.copy(id = stableId)
            eventDao.insert(
                storedEvent.toEntity().copy(
                    id = stableId,
                    userId = userId,
                ),
            )
            scheduleReminderIfNeeded(storedEvent, userId)
            syncMutationEnqueuer.enqueueUpsert(
                entityType = "event",
                entityId = stableId.toString(),
            )
            return stableId
        }

        override suspend fun updateEvent(event: CalendarEvent) {
            val userId = activeUserId()
            eventDao.update(event.toEntity().copy(userId = userId))
            scheduleReminderIfNeeded(event, userId)
            syncMutationEnqueuer.enqueueUpsert(
                entityType = "event",
                entityId = event.id.toString(),
            )
        }

        override suspend fun deleteEvent(event: CalendarEvent) {
            val userId = activeUserId()
            reminderScheduler.cancelEventReminder(event.id, userId)
            eventDao.delete(event.toEntity().copy(userId = userId))
            syncMutationEnqueuer.enqueueDelete(
                entityType = "event",
                entityId = event.id.toString(),
            )
        }

        override suspend fun getById(id: Long): CalendarEvent? {
            return eventDao.getById(id, activeUserId())?.toDomain()
        }

        private suspend fun scheduleReminderIfNeeded(
            event: CalendarEvent,
            userId: String,
        ) {
            if (event.hasReminder && event.status == EventStatus.PENDING) {
                reminderScheduler.scheduleEventReminder(event = event, userId = userId)
            } else {
                reminderScheduler.cancelEventReminder(eventId = event.id, userId = userId)
            }
        }
    }

fun EventEntity.toDomain(): CalendarEvent {
    return CalendarEvent(
        id = id, title = title, description = description,
        date = date, endDate = endDate,
        type =
            try {
                EventType.valueOf(type)
            } catch (_: Exception) {
                EventType.OTHER
            },
        importance =
            try {
                EventImportance.valueOf(importance)
            } catch (_: Exception) {
                EventImportance.NEUTRAL
            },
        status =
            try {
                EventStatus.valueOf(status)
            } catch (_: Exception) {
                EventStatus.PENDING
            },
        hasReminder = hasReminder, reminderMinutesBefore = reminderMinutesBefore,
        createdAt = createdAt,
        kind = try { EventKind.valueOf(kind) } catch (_: Exception) { EventKind.EVENT },
        allDay = allDay,
        repeatRule = try { RepeatRule.valueOf(repeatRule) } catch (_: Exception) { RepeatRule.NEVER },
        reminderOffsets = if (reminderOffsets.isBlank()) emptyList()
            else reminderOffsets.split(",").mapNotNull { it.trim().toIntOrNull() },
        alarmEnabled = alarmEnabled,
        guests = guests,
        timeZoneId = timeZoneId,
    )
}

fun CalendarEvent.toEntity(): EventEntity {
    return EventEntity(
        id = id, title = title, description = description,
        date = date, endDate = endDate, type = type.name,
        importance = importance.name,
        status = status.name,
        hasReminder = hasReminder, reminderMinutesBefore = reminderMinutesBefore,
        createdAt = createdAt,
        kind = kind.name,
        allDay = allDay,
        repeatRule = repeatRule.name,
        reminderOffsets = reminderOffsets.joinToString(","),
        alarmEnabled = alarmEnabled,
        guests = guests,
        timeZoneId = timeZoneId,
    )
}
