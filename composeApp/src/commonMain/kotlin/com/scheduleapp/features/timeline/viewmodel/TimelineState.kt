package com.scheduleapp.features.timeline.viewmodel

import com.scheduleapp.core.database.entity.MaintenanceItemEntity
import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.calendar.model.ScheduleEvent
import com.scheduleapp.domain.calendar.model.TodoItem
import kotlinx.datetime.LocalDate

sealed interface TimelineItem {
    val sortKey: Long

    data class EventItem(
        val event: ScheduleEvent
    ) : TimelineItem {
        override val sortKey: Long get() = event.startTime
    }

    data class TodoTimelineItem(
        val todo: TodoItem
    ) : TimelineItem {
        override val sortKey: Long get() = todo.dueDate ?: Long.MAX_VALUE
    }

    data class CarAlertItem(
        val maintenanceItem: MaintenanceItemEntity,
        val vehicleName: String,
        val message: String,
        val isOverdue: Boolean
    ) : TimelineItem {
        override val sortKey: Long get() = maintenanceItem.nextDueDate ?: Long.MAX_VALUE
    }
}

data class TimelineState(
    val selectedDate: LocalDate = DateTimeUtils.today(),
    val items: List<TimelineItem> = emptyList(),
    val eventCount: Int = 0,
    val todoCount: Int = 0,
    val carAlertCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
