package com.scheduleapp.domain.calendar.model

data class ScheduleEvent(
    val id: Long = 0,
    val externalEventId: String? = null,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val isAllDay: Boolean = false,
    val category: String = "GENERAL",
    val linkedCarRecordId: Long? = null,
    val memo: String? = null
) {
    init {
        require(endTime >= startTime) { "Event endTime cannot be earlier than startTime" }
    }
}
