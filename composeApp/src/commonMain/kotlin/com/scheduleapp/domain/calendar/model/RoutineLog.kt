package com.scheduleapp.domain.calendar.model

data class RoutineLog(
    val id: Long = 0,
    val routineId: Long,
    val date: String, // "YYYY-MM-DD"
    val isCompleted: Boolean = true,
    val completedAt: Long? = null
)
