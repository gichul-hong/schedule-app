package com.scheduleapp.features.todo.viewmodel

import com.scheduleapp.domain.calendar.model.Priority
import com.scheduleapp.domain.calendar.model.TodoItem
import com.scheduleapp.domain.calendar.repository.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TodoViewModel(
    private val repository: ScheduleRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _state = MutableStateFlow(TodoState())
    val state: StateFlow<TodoState> = _state.asStateFlow()

    init {
        observeTodos()
    }

    private fun observeTodos() {
        scope.launch {
            repository.getAllTodos().collectLatest { allTodos ->
                _state.update { current ->
                    current.copy(
                        todos = allTodos,
                        filteredTodos = applyFilters(allTodos, current.activeFilter, current.selectedPriority)
                    )
                }
            }
        }
    }

    fun setFilter(filter: TodoFilter) {
        _state.update { current ->
            current.copy(
                activeFilter = filter,
                filteredTodos = applyFilters(current.todos, filter, current.selectedPriority)
            )
        }
    }

    fun setPriorityFilter(priority: Priority?) {
        _state.update { current ->
            current.copy(
                selectedPriority = priority,
                filteredTodos = applyFilters(current.todos, current.activeFilter, priority)
            )
        }
    }

    fun addTodo(
        title: String,
        dueDate: Long? = null,
        priority: Priority = Priority.MEDIUM
    ) {
        if (title.isBlank()) return
        scope.launch {
            try {
                val item = TodoItem(
                    title = title.trim(),
                    dueDate = dueDate,
                    priority = priority,
                    isCompleted = false
                )
                repository.insertTodo(item)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun toggleCompletion(id: Long, isCompleted: Boolean) {
        scope.launch {
            try {
                repository.toggleTodoCompletion(id, isCompleted)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteTodo(todo: TodoItem) {
        scope.launch {
            try {
                repository.deleteTodo(todo)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun updateTodo(todo: TodoItem) {
        scope.launch {
            try {
                repository.updateTodo(todo)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun applyFilters(
        items: List<TodoItem>,
        filter: TodoFilter,
        priority: Priority?
    ): List<TodoItem> {
        return items.filter { item ->
            val matchesFilter = when (filter) {
                TodoFilter.ALL -> true
                TodoFilter.ACTIVE -> !item.isCompleted
                TodoFilter.COMPLETED -> item.isCompleted
            }
            val matchesPriority = priority == null || item.priority == priority
            matchesFilter && matchesPriority
        }.sortedWith(
            compareBy<TodoItem> { it.isCompleted }
                .thenByDescending { it.priority.level }
                .thenBy { it.dueDate ?: Long.MAX_VALUE }
        )
    }
}
