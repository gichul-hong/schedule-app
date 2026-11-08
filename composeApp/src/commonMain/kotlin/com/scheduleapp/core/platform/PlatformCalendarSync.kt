package com.scheduleapp.core.platform

import com.scheduleapp.domain.calendar.model.ScheduleEvent

expect class PlatformCalendarSync() {
    suspend fun syncEvent(event: ScheduleEvent): Boolean
    suspend fun deleteEvent(externalId: String): Boolean
    fun checkPermission(): Boolean
    suspend fun requestPermission(): Boolean
}
