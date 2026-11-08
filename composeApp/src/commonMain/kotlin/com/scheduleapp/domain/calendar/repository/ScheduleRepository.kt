package com.scheduleapp.domain.calendar.repository

import com.scheduleapp.domain.calendar.model.Routine
import com.scheduleapp.domain.calendar.model.RoutineLog
import com.scheduleapp.domain.calendar.model.ScheduleEvent
import com.scheduleapp.domain.calendar.model.TodoItem
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    // Events
    fun getAllEvents(): Flow<List<ScheduleEvent>>
    fun getEventsInRange(start: Long, end: Long): Flow<List<ScheduleEvent>>
    suspend fun getEventById(id: Long): ScheduleEvent?
    suspend fun insertEvent(event: ScheduleEvent): Long
    suspend fun updateEvent(event: ScheduleEvent)
    suspend fun deleteEvent(event: ScheduleEvent)

    // Todos
    fun getAllTodos(): Flow<List<TodoItem>>
    fun getTodosByDate(start: Long, end: Long): Flow<List<TodoItem>>
    suspend fun getTodoById(id: Long): TodoItem?
    suspend fun insertTodo(todo: TodoItem): Long
    suspend fun updateTodo(todo: TodoItem)
    suspend fun deleteTodo(todo: TodoItem)
    suspend fun toggleTodoCompletion(id: Long, isCompleted: Boolean)

    // Routines
    fun getAllRoutines(): Flow<List<Routine>>
    suspend fun getRoutineById(id: Long): Routine?
    suspend fun insertRoutine(routine: Routine): Long
    suspend fun updateRoutine(routine: Routine)
    suspend fun deleteRoutine(routine: Routine)

    // Routine Logs
    fun getAllRoutineLogs(): Flow<List<RoutineLog>>
    fun getRoutineLogsForDate(date: String): Flow<List<RoutineLog>>
    fun getRoutineLogsForRoutine(routineId: Long): Flow<List<RoutineLog>>
    suspend fun insertRoutineLog(log: RoutineLog): Long
    suspend fun updateRoutineLog(log: RoutineLog)
    suspend fun deleteRoutineLog(log: RoutineLog)
    suspend fun toggleRoutineCompletion(routineId: Long, date: String, isCompleted: Boolean)
}
