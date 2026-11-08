package com.scheduleapp.features.routine.viewmodel

import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.calendar.model.Routine
import kotlinx.datetime.LocalDate

data class RoutineItemUiState(
    val routine: Routine,
    val isScheduledForDate: Boolean,
    val isCompletedForDate: Boolean,
    val streak: Int
)

data class RoutineState(
    val selectedDate: LocalDate = DateTimeUtils.today(),
    val habits: List<RoutineItemUiState> = emptyList(),
    val scheduledHabits: List<RoutineItemUiState> = emptyList(),
    val completionRate: Float = 0f,
    val completedCount: Int = 0,
    val scheduledCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
