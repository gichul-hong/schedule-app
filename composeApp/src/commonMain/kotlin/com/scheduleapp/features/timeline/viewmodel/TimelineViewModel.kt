package com.scheduleapp.features.timeline.viewmodel

import com.scheduleapp.core.database.dao.CarBookDao
import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.calendar.model.ScheduleEvent
import com.scheduleapp.domain.calendar.repository.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class TimelineViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val carBookDao: CarBookDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _state = MutableStateFlow(TimelineState())
    val state: StateFlow<TimelineState> = _state.asStateFlow()

    init {
        loadTimelineForDate(_state.value.selectedDate)
    }

    fun selectDate(date: LocalDate) {
        _state.update { it.copy(selectedDate = date) }
        loadTimelineForDate(date)
    }

    fun navigateDay(delta: Int) {
        val current = _state.value.selectedDate
        val target = LocalDate.fromEpochDays(current.toEpochDays() + delta)
        selectDate(target)
    }

    fun toggleTodo(todoId: Long, isCompleted: Boolean) {
        scope.launch {
            try {
                scheduleRepository.toggleTodoCompletion(todoId, isCompleted)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteEvent(event: ScheduleEvent) {
        scope.launch {
            try {
                scheduleRepository.deleteEvent(event)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun loadTimelineForDate(date: LocalDate) {
        val startOfDay = DateTimeUtils.startOfDayMillis(date)
        val endOfDay = DateTimeUtils.endOfDayMillis(date)

        scope.launch {
            try {
                val defaultVehicle = carBookDao.getDefaultVehicle().firstOrNull()

                val maintenanceFlow = if (defaultVehicle != null) {
                    carBookDao.getMaintenanceItemsForVehicle(defaultVehicle.id)
                } else {
                    kotlinx.coroutines.flow.flowOf(emptyList())
                }

                combine(
                    scheduleRepository.getEventsInRange(startOfDay, endOfDay),
                    scheduleRepository.getAllTodos(),
                    maintenanceFlow
                ) { events, todos, maintenanceItems ->
                    val eventItems = events.map { TimelineItem.EventItem(it) }

                    // Todos due on this date or completed on this date
                    val todoItems = todos.filter { todo ->
                        val due = todo.dueDate
                        if (due != null) {
                            due in startOfDay..endOfDay
                        } else {
                            // If no due date, show active todos on today's timeline
                            date == DateTimeUtils.today() && !todo.isCompleted
                        }
                    }.map { TimelineItem.TodoTimelineItem(it) }

                    // Car maintenance alerts due on or before this day
                    val carItems = maintenanceItems.filter { item ->
                        val nextDue = item.nextDueDate
                        if (nextDue != null) {
                            nextDue <= endOfDay
                        } else {
                            false
                        }
                    }.map { item ->
                        val isOverdue = (item.nextDueDate ?: Long.MAX_VALUE) < startOfDay
                        val vehicleName = defaultVehicle?.name ?: "내 차"
                        val msg = if (isOverdue) {
                            "교체 기한 초과: ${item.partName}"
                        } else {
                            "교체 예정일: ${item.partName}"
                        }
                        TimelineItem.CarAlertItem(
                            maintenanceItem = item,
                            vehicleName = vehicleName,
                            message = msg,
                            isOverdue = isOverdue
                        )
                    }

                    val allItems = (eventItems + todoItems + carItems).sortedBy { it.sortKey }

                    TimelineState(
                        selectedDate = date,
                        items = allItems,
                        eventCount = eventItems.size,
                        todoCount = todoItems.size,
                        carAlertCount = carItems.size,
                        isLoading = false
                    )
                }.collect { newState ->
                    _state.value = newState
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
        }
    }
}
