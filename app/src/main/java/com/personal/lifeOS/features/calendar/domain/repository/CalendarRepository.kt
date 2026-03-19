package com.personal.lifeOS.features.calendar.domain.repository

import com.personal.lifeOS.features.calendar.domain.model.CalendarEvent
import kotlinx.coroutines.flow.Flow

interface CalendarRepository {
    fun getEventsBetween(
        start: Long,
        end: Long,
    ): Flow<List<CalendarEvent>>

    fun getUpcomingEvents(limit: Int = 10): Flow<List<CalendarEvent>>

    fun getByType(type: String): Flow<List<CalendarEvent>>

    suspend fun addEvent(event: CalendarEvent): Long

    suspend fun updateEvent(event: CalendarEvent)

    suspend fun deleteEvent(event: CalendarEvent)

    suspend fun getById(id: Long): CalendarEvent?
}
