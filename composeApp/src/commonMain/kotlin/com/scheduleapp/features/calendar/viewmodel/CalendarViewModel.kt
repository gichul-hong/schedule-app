package com.scheduleapp.features.calendar.viewmodel

import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.calendar.model.ScheduleEvent
import com.scheduleapp.domain.calendar.repository.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class CalendarViewModel(
    private val repository: ScheduleRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    init {
        loadEventsForMonth(_state.value.currentMonth)
    }

    fun selectDate(date: LocalDate) {
        _state.update { it.copy(selectedDate = date) }
        refreshSelectedDayEvents()
    }

    fun setViewMode(mode: CalendarViewMode) {
        _state.update { it.copy(viewMode = mode) }
    }

    fun selectCategory(category: String?) {
        val cat = if (category == "ALL") null else category
        _state.update { it.copy(selectedCategory = cat) }
        refreshSelectedDayEvents()
    }

    fun navigateMonth(delta: Int) {
        val current = _state.value.currentMonth
        var newMonth = current.monthNumber + delta
        var newYear = current.year
        while (newMonth < 1) {
            newMonth += 12
            newYear -= 1
        }
        while (newMonth > 12) {
            newMonth -= 12
            newYear += 1
        }
        val target = LocalDate(newYear, newMonth, 1)
        _state.update { it.copy(currentMonth = target, selectedDate = target) }
        loadEventsForMonth(target)
    }

    fun createEvent(
        title: String,
        startTime: Long,
        endTime: Long,
        isAllDay: Boolean = false,
        category: String = "GENERAL",
        memo: String? = null,
        linkedCarRecordId: Long? = null
    ) {
        scope.launch {
            try {
                val event = ScheduleEvent(
                    title = title.trim(),
                    startTime = startTime,
                    endTime = endTime,
                    isAllDay = isAllDay,
                    category = category,
                    memo = memo?.trim(),
                    linkedCarRecordId = linkedCarRecordId
                )
                repository.insertEvent(event)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun updateEvent(event: ScheduleEvent) {
        scope.launch {
            try {
                repository.updateEvent(event)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteEvent(event: ScheduleEvent) {
        scope.launch {
            try {
                repository.deleteEvent(event)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun loadEventsForMonth(monthDate: LocalDate) {
        scope.launch {
            val start = DateTimeUtils.startOfDayMillis(DateTimeUtils.startOfMonth(monthDate.year, monthDate.monthNumber))
            val end = DateTimeUtils.endOfDayMillis(DateTimeUtils.endOfMonth(monthDate.year, monthDate.monthNumber))
            repository.getEventsInRange(start, end).collectLatest { events ->
                _state.update { it.copy(rangeEvents = events) }
                refreshSelectedDayEvents()
            }
        }
    }

    private fun refreshSelectedDayEvents() {
        val selected = _state.value.selectedDate
        val dayStart = DateTimeUtils.startOfDayMillis(selected)
        val dayEnd = DateTimeUtils.endOfDayMillis(selected)
        val categoryFilter = _state.value.selectedCategory

        val filtered = _state.value.rangeEvents.filter { event ->
            val inRange = event.startTime <= dayEnd && event.endTime >= dayStart
            val matchesCategory = categoryFilter == null || event.category.equals(categoryFilter, ignoreCase = true)
            inRange && matchesCategory
        }
        _state.update { it.copy(selectedDayEvents = filtered) }
    }
}
