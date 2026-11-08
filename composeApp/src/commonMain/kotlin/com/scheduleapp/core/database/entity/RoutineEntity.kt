package com.scheduleapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val repeatDaysMask: Int = 127,
    val targetTime: String? = null,
    val category: String = "DAILY",
    val createdAt: Long
)
