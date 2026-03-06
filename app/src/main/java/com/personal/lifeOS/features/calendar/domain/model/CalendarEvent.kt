package com.personal.lifeOS.features.calendar.domain.model

data class CalendarEvent(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val date: Long,
    val endDate: Long? = null,
    val type: EventType = EventType.PERSONAL,
    val hasReminder: Boolean = false,
    val reminderMinutesBefore: Int = 15,
    val createdAt: Long = System.currentTimeMillis()
)

enum class EventType(val label: String) {
    WORK("Work"),
    PERSONAL("Personal"),
    HEALTH("Health"),
    FINANCE("Finance"),
    OTHER("Other")
}
