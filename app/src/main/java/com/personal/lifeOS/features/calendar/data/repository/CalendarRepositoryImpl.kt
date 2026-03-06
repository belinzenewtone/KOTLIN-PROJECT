package com.personal.lifeOS.features.calendar.data.repository

import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.entity.EventEntity
import com.personal.lifeOS.features.calendar.domain.model.CalendarEvent
import com.personal.lifeOS.features.calendar.domain.model.EventType
import com.personal.lifeOS.features.calendar.domain.repository.CalendarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepositoryImpl @Inject constructor(
    private val eventDao: EventDao
) : CalendarRepository {

    override fun getEventsBetween(start: Long, end: Long): Flow<List<CalendarEvent>> {
        return eventDao.getEventsBetween(start, end).map { it.map { e -> e.toDomain() } }
    }

    override fun getUpcomingEvents(limit: Int): Flow<List<CalendarEvent>> {
        return eventDao.getUpcomingEvents(System.currentTimeMillis(), limit).map { it.map { e -> e.toDomain() } }
    }

    override fun getByType(type: String): Flow<List<CalendarEvent>> {
        return eventDao.getByType(type).map { it.map { e -> e.toDomain() } }
    }

    override suspend fun addEvent(event: CalendarEvent): Long {
        return eventDao.insert(event.toEntity())
    }

    override suspend fun updateEvent(event: CalendarEvent) {
        eventDao.update(event.toEntity())
    }

    override suspend fun deleteEvent(event: CalendarEvent) {
        eventDao.delete(event.toEntity())
    }

    override suspend fun getById(id: Long): CalendarEvent? {
        return eventDao.getById(id)?.toDomain()
    }
}

fun EventEntity.toDomain(): CalendarEvent {
    return CalendarEvent(
        id = id, title = title, description = description,
        date = date, endDate = endDate,
        type = try { EventType.valueOf(type) } catch (_: Exception) { EventType.OTHER },
        hasReminder = hasReminder, reminderMinutesBefore = reminderMinutesBefore,
        createdAt = createdAt
    )
}

fun CalendarEvent.toEntity(): EventEntity {
    return EventEntity(
        id = id, title = title, description = description,
        date = date, endDate = endDate, type = type.name,
        hasReminder = hasReminder, reminderMinutesBefore = reminderMinutesBefore,
        createdAt = createdAt
    )
}
