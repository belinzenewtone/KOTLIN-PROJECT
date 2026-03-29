package com.personal.lifeOS.features.calendar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lifeOS.features.calendar.domain.model.CalendarEvent
import com.personal.lifeOS.features.calendar.domain.model.EventImportance
import com.personal.lifeOS.features.calendar.domain.model.EventStatus
import com.personal.lifeOS.features.calendar.domain.model.EventType
import com.personal.lifeOS.features.calendar.domain.repository.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val events: List<CalendarEvent> = emptyList(),
    val selectedDayEvents: List<CalendarEvent> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val editingEvent: CalendarEvent? = null,
    /** True when the user is viewing the current real-world month. */
    val isViewingCurrentMonth: Boolean = true,
)

@HiltViewModel
class CalendarViewModel
    @Inject
    constructor(
        private val repository: CalendarRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CalendarUiState())
        val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

        init {
            loadMonthEvents()
        }

        fun navigateMonth(offset: Int) {
            val newMonth = _uiState.value.currentMonth.plusMonths(offset.toLong())
            _uiState.update { it.copy(
                currentMonth = newMonth,
                isViewingCurrentMonth = newMonth == YearMonth.now(),
            ) }
            loadMonthEvents()
        }

        /** Jump selected date and displayed month back to today. */
        fun goToToday() {
            val todayMonth = YearMonth.now()
            _uiState.update { it.copy(
                currentMonth = todayMonth,
                selectedDate = LocalDate.now(),
                isViewingCurrentMonth = true,
            ) }
            loadMonthEvents()
        }

        fun selectDate(date: LocalDate) {
            _uiState.update { it.copy(selectedDate = date) }
            filterSelectedDayEvents()
        }

        fun showAddDialog() {
            _uiState.update { it.copy(showAddDialog = true, editingEvent = null) }
        }

        fun showEditDialog(event: CalendarEvent) {
            _uiState.update { it.copy(showAddDialog = true, editingEvent = event) }
        }

        fun hideAddDialog() {
            _uiState.update { it.copy(showAddDialog = false, editingEvent = null) }
        }

        fun saveEvent(
            title: String,
            description: String,
            type: EventType,
            importance: EventImportance,
            date: Long,
            endDate: Long?,
            hasReminder: Boolean = false,
            reminderMinutesBefore: Int = 15,
        ) {
            viewModelScope.launch {
                try {
                    val editing = _uiState.value.editingEvent
                    if (editing == null) {
                        repository.addEvent(
                            CalendarEvent(
                                title = title,
                                description = description,
                                date = date,
                                endDate = endDate,
                                type = type,
                                importance = importance,
                                status = EventStatus.PENDING,
                                hasReminder = hasReminder,
                                reminderMinutesBefore = reminderMinutesBefore,
                            ),
                        )
                    } else {
                        repository.updateEvent(
                            editing.copy(
                                title = title,
                                description = description,
                                date = date,
                                endDate = endDate,
                                type = type,
                                importance = importance,
                                hasReminder = hasReminder,
                                reminderMinutesBefore = reminderMinutesBefore,
                            ),
                        )
                    }
                    _uiState.update { it.copy(showAddDialog = false, editingEvent = null) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }

        fun markEventCompleted(event: CalendarEvent) {
            if (event.status == EventStatus.COMPLETED) return
            viewModelScope.launch {
                try {
                    repository.updateEvent(
                        event.copy(
                            status = EventStatus.COMPLETED,
                            hasReminder = false,
                        ),
                    )
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }

        fun deleteEvent(event: CalendarEvent) {
            viewModelScope.launch {
                try {
                    repository.deleteEvent(event)
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }

        private fun loadMonthEvents() {
            val month = _uiState.value.currentMonth
            val zone = ZoneId.systemDefault()
            val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = month.atEndOfMonth().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

            repository.getEventsBetween(start, end)
                .onEach { events ->
                    _uiState.update { it.copy(events = events, isLoading = false) }
                    filterSelectedDayEvents()
                }
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .launchIn(viewModelScope)
        }

        private fun filterSelectedDayEvents() {
            val state = _uiState.value
            val zone = ZoneId.systemDefault()
            val dayStart = state.selectedDate.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = state.selectedDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

            val dayEvents =
                state.events
                    .filter { it.date in dayStart..dayEnd }
                    .sortedBy { it.date }
            _uiState.update { it.copy(selectedDayEvents = dayEvents) }
        }
    }
