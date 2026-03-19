package com.personal.lifeOS.features.calendar.presentation

import java.time.LocalDate

sealed interface CalendarUiEvent {
    data class SelectDate(val date: LocalDate) : CalendarUiEvent

    data class NavigateMonth(val offset: Int) : CalendarUiEvent

    data object AddEvent : CalendarUiEvent

    data class EditEvent(val eventId: Long) : CalendarUiEvent

    data class CompleteEvent(val eventId: Long) : CalendarUiEvent

    data class DeleteEvent(val eventId: Long) : CalendarUiEvent
}
