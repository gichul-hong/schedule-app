package com.scheduleapp.features.routine.viewmodel

import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.calendar.engine.RoutineStreakEngine
import com.scheduleapp.domain.calendar.model.Routine
import com.scheduleapp.domain.calendar.model.RoutineLog
import com.scheduleapp.domain.calendar.repository.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class RoutineViewModel(
    private val repository: ScheduleRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _state = MutableStateFlow(RoutineState())
    val state: StateFlow<RoutineState> = _state.asStateFlow()

    private var cachedRoutines: List<Routine> = emptyList()
    private var cachedLogs: List<RoutineLog> = emptyList()

    init {
        observeRoutinesAndLogs()
    }

    private fun observeRoutinesAndLogs() {
        scope.launch {
            combine(
                repository.getAllRoutines(),
                repository.getAllRoutineLogs()
            ) { routines, logs ->
                cachedRoutines = routines
                cachedLogs = logs
                computeState(_state.value.selectedDate, routines, logs)
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _state.update { computeState(date, cachedRoutines, cachedLogs) }
    }

    fun toggleRoutine(routineId: Long, isCompleted: Boolean) {
        scope.launch {
            try {
                val dateStr = DateTimeUtils.formatDate(_state.value.selectedDate)
                repository.toggleRoutineCompletion(routineId, dateStr, isCompleted)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun createRoutine(
        title: String,
        repeatDaysMask: Int = 127,
        targetTime: String? = null,
        category: String = "DAILY"
    ) {
        if (title.isBlank()) return
        scope.launch {
            try {
                val routine = Routine(
                    title = title.trim(),
                    repeatDaysMask = repeatDaysMask,
                    targetTime = targetTime?.trim(),
                    category = category.trim(),
                    createdAt = DateTimeUtils.nowMillis()
                )
                repository.insertRoutine(routine)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteRoutine(routine: Routine) {
        scope.launch {
            try {
                repository.deleteRoutine(routine)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun updateRoutine(routine: Routine) {
        scope.launch {
            try {
                repository.updateRoutine(routine)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun computeState(
        date: LocalDate,
        routines: List<Routine>,
        logs: List<RoutineLog>
    ): RoutineState {
        val dateStr = DateTimeUtils.formatDate(date)
        val logsForDate = logs.filter { it.date == dateStr }

        val items = routines.map { routine ->
            val isScheduled = RoutineStreakEngine.isScheduledOnDate(routine.repeatDaysMask, date)
            val isCompleted = logsForDate.any { it.routineId == routine.id && it.isCompleted }
            val streak = RoutineStreakEngine.calculateStreak(
                routine = routine,
                logs = logs,
                referenceDate = date,
                allowTodayPending = true
            )
            RoutineItemUiState(
                routine = routine,
                isScheduledForDate = isScheduled,
                isCompletedForDate = isCompleted,
                streak = streak
            )
        }

        val scheduledItems = items.filter { it.isScheduledForDate }
        val completedCount = scheduledItems.count { it.isCompletedForDate }
        val scheduledCount = scheduledItems.size
        val completionRate = if (scheduledCount > 0) completedCount.toFloat() / scheduledCount.toFloat() else 0f

        return RoutineState(
            selectedDate = date,
            habits = items,
            scheduledHabits = scheduledItems,
            completionRate = completionRate,
            completedCount = completedCount,
            scheduledCount = scheduledCount
        )
    }
}
