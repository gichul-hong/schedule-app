package com.scheduleapp.features.todo.viewmodel

import com.scheduleapp.domain.calendar.model.Priority
import com.scheduleapp.domain.calendar.model.TodoItem

enum class TodoFilter { ALL, ACTIVE, COMPLETED }

data class TodoState(
    val todos: List<TodoItem> = emptyList(),
    val filteredTodos: List<TodoItem> = emptyList(),
    val activeFilter: TodoFilter = TodoFilter.ALL,
    val selectedPriority: Priority? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
