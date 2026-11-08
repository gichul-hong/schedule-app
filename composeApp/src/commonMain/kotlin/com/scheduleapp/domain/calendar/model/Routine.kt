package com.scheduleapp.domain.calendar.model

data class Routine(
    val id: Long = 0,
    val title: String,
    val repeatDaysMask: Int = 127, // Bitmask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64
    val targetTime: String? = null,
    val category: String = "DAILY",
    val createdAt: Long = 0
)
