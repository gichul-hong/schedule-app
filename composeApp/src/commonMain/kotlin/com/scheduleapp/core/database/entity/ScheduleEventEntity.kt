package com.scheduleapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_events")
data class ScheduleEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val externalEventId: String? = null,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val isAllDay: Boolean = false,
    val category: String = "GENERAL",
    val linkedCarRecordId: Long? = null,
    val memo: String? = null
)
