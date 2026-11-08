package com.scheduleapp.domain.calendar.model

data class TodoItem(
    val id: Long = 0,
    val title: String,
    val dueDate: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)
