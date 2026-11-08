package com.scheduleapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_items")
data class TodoItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val dueDate: Long? = null,
    val priority: Int = 2,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)
