package com.scheduleapp.domain.calendar.model

enum class Priority(val level: Int, val displayName: String) {
    LOW(0, "낮음"),
    MEDIUM(1, "보통"),
    HIGH(2, "높음"),
    URGENT(3, "긴급");

    companion object {
        fun fromInt(value: Int): Priority = entries.firstOrNull { it.level == value } ?: MEDIUM
    }
}
