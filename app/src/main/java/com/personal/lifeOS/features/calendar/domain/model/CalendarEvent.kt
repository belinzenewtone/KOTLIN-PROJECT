package com.personal.lifeOS.features.calendar.domain.model

data class CalendarEvent(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val date: Long,
    val endDate: Long? = null,
    val type: EventType = EventType.PERSONAL,
    val importance: EventImportance = EventImportance.NEUTRAL,
    val status: EventStatus = EventStatus.PENDING,
    val hasReminder: Boolean = false,
    val reminderMinutesBefore: Int = 15,
    val createdAt: Long = System.currentTimeMillis(),
    // Extended fields
    val kind: EventKind = EventKind.EVENT,
    val allDay: Boolean = false,
    val repeatRule: RepeatRule = RepeatRule.NEVER,
    val reminderOffsets: List<Int> = emptyList(),
    val alarmEnabled: Boolean = false,
    val guests: String = "",
    val timeZoneId: String = "",
    /** Minutes from midnight used as the time-of-day for all-day event reminders (default 08:00 = 480). */
    val reminderTimeOfDayMinutes: Int = 480,
)

enum class EventKind(val label: String) {
    EVENT("Event"),
    BIRTHDAY("Birthday"),
    ANNIVERSARY("Anniversary"),
    COUNTDOWN("Countdown"),
}

enum class RepeatRule(val label: String) {
    NEVER("Never"),
    DAILY("Daily"),
    MON_FRI("Mon – Fri"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
}

enum class EventType(val label: String) {
    WORK("Work"),
    PERSONAL("Personal"),
    HEALTH("Health"),
    FINANCE("Finance"),
    OTHER("Other"),
}

enum class EventImportance(val label: String) {
    NEUTRAL("Neutral"),
    IMPORTANT("Important"),
    URGENT("Urgent"),
}

enum class EventStatus {
    PENDING,
    COMPLETED,
}
