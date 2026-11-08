package com.scheduleapp.features.calendar.viewmodel

import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.calendar.model.ScheduleEvent
import kotlinx.datetime.LocalDate

enum class CalendarViewMode { MONTH, WEEK, DAY }

data class CalendarState(
    val selectedDate: LocalDate = DateTimeUtils.today(),
    val currentMonth: LocalDate = DateTimeUtils.today(),
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val selectedCategory: String? = null,
    val rangeEvents: List<ScheduleEvent> = emptyList(),
    val selectedDayEvents: List<ScheduleEvent> = emptyList(),
    val categories: List<String> = listOf("ALL", "GENERAL", "WORK", "PERSONAL", "CAR"),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
