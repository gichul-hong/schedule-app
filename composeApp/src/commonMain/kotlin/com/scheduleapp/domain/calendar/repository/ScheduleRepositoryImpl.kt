package com.scheduleapp.domain.calendar.repository

import com.scheduleapp.core.database.dao.ScheduleDao
import com.scheduleapp.core.database.entity.RoutineEntity
import com.scheduleapp.core.database.entity.RoutineLogEntity
import com.scheduleapp.core.database.entity.ScheduleEventEntity
import com.scheduleapp.core.database.entity.TodoItemEntity
import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.calendar.model.Priority
import com.scheduleapp.domain.calendar.model.Routine
import com.scheduleapp.domain.calendar.model.RoutineLog
import com.scheduleapp.domain.calendar.model.ScheduleEvent
import com.scheduleapp.domain.calendar.model.TodoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScheduleRepositoryImpl(
    private val scheduleDao: ScheduleDao
) : ScheduleRepository {

    // --- Schedule Events ---

    override fun getAllEvents(): Flow<List<ScheduleEvent>> {
        return scheduleDao.getAllEvents().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getEventsInRange(start: Long, end: Long): Flow<List<ScheduleEvent>> {
        return scheduleDao.getEventsInRange(start, end).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getEventById(id: Long): ScheduleEvent? {
        return scheduleDao.getEventById(id)?.toDomain()
    }

    override suspend fun insertEvent(event: ScheduleEvent): Long {
        require(event.endTime >= event.startTime) { "Event endTime cannot be earlier than startTime" }
        return scheduleDao.insertEvent(event.toEntity())
    }

    override suspend fun updateEvent(event: ScheduleEvent) {
        require(event.endTime >= event.startTime) { "Event endTime cannot be earlier than startTime" }
        scheduleDao.updateEvent(event.toEntity())
    }

    override suspend fun deleteEvent(event: ScheduleEvent) {
        scheduleDao.deleteEvent(event.toEntity())
    }

    // --- Todo Items ---

    override fun getAllTodos(): Flow<List<TodoItem>> {
        return scheduleDao.getAllTodos().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTodosByDate(start: Long, end: Long): Flow<List<TodoItem>> {
        return scheduleDao.getTodosByDate(start, end).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTodoById(id: Long): TodoItem? {
        return scheduleDao.getTodoById(id)?.toDomain()
    }

    override suspend fun insertTodo(todo: TodoItem): Long {
        return scheduleDao.insertTodo(todo.toEntity())
    }

    override suspend fun updateTodo(todo: TodoItem) {
        scheduleDao.updateTodo(todo.toEntity())
    }

    override suspend fun deleteTodo(todo: TodoItem) {
        scheduleDao.deleteTodo(todo.toEntity())
    }

    override suspend fun toggleTodoCompletion(id: Long, isCompleted: Boolean) {
        val current = scheduleDao.getTodoById(id) ?: return
        val updated = current.copy(
            isCompleted = isCompleted,
            completedAt = if (isCompleted) DateTimeUtils.nowMillis() else null
        )
        scheduleDao.updateTodo(updated)
    }

    // --- Routines ---

    override fun getAllRoutines(): Flow<List<Routine>> {
        return scheduleDao.getAllRoutines().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getRoutineById(id: Long): Routine? {
        return scheduleDao.getRoutineById(id)?.toDomain()
    }

    override suspend fun insertRoutine(routine: Routine): Long {
        return scheduleDao.insertRoutine(routine.toEntity())
    }

    override suspend fun updateRoutine(routine: Routine) {
        scheduleDao.updateRoutine(routine.toEntity())
    }

    override suspend fun deleteRoutine(routine: Routine) {
        scheduleDao.deleteRoutine(routine.toEntity())
    }

    // --- Routine Logs ---

    override fun getAllRoutineLogs(): Flow<List<RoutineLog>> {
        return scheduleDao.getAllRoutineLogs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRoutineLogsForDate(date: String): Flow<List<RoutineLog>> {
        return scheduleDao.getRoutineLogsForDate(date).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRoutineLogsForRoutine(routineId: Long): Flow<List<RoutineLog>> {
        return scheduleDao.getRoutineLogsForRoutine(routineId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertRoutineLog(log: RoutineLog): Long {
        return scheduleDao.insertRoutineLog(log.toEntity())
    }

    override suspend fun updateRoutineLog(log: RoutineLog) {
        scheduleDao.updateRoutineLog(log.toEntity())
    }

    override suspend fun deleteRoutineLog(log: RoutineLog) {
        scheduleDao.deleteRoutineLog(log.toEntity())
    }

    override suspend fun toggleRoutineCompletion(routineId: Long, date: String, isCompleted: Boolean) {
        val existing = scheduleDao.getRoutineLog(routineId, date)
        if (existing != null) {
            if (isCompleted) {
                scheduleDao.updateRoutineLog(
                    existing.copy(isCompleted = true, completedAt = DateTimeUtils.nowMillis())
                )
            } else {
                scheduleDao.deleteRoutineLog(existing)
            }
        } else if (isCompleted) {
            scheduleDao.insertRoutineLog(
                RoutineLogEntity(
                    routineId = routineId,
                    date = date,
                    isCompleted = true,
                    completedAt = DateTimeUtils.nowMillis()
                )
            )
        }
    }

    // --- Entity Mappers ---

    private fun ScheduleEventEntity.toDomain(): ScheduleEvent = ScheduleEvent(
        id = id,
        externalEventId = externalEventId,
        title = title,
        startTime = startTime,
        endTime = endTime,
        isAllDay = isAllDay,
        category = category,
        linkedCarRecordId = linkedCarRecordId,
        memo = memo
    )

    private fun ScheduleEvent.toEntity(): ScheduleEventEntity = ScheduleEventEntity(
        id = id,
        externalEventId = externalEventId,
        title = title,
        startTime = startTime,
        endTime = endTime,
        isAllDay = isAllDay,
        category = category,
        linkedCarRecordId = linkedCarRecordId,
        memo = memo
    )

    private fun TodoItemEntity.toDomain(): TodoItem = TodoItem(
        id = id,
        title = title,
        dueDate = dueDate,
        priority = Priority.fromInt(priority),
        isCompleted = isCompleted,
        completedAt = completedAt
    )

    private fun TodoItem.toEntity(): TodoItemEntity = TodoItemEntity(
        id = id,
        title = title,
        dueDate = dueDate,
        priority = priority.level,
        isCompleted = isCompleted,
        completedAt = completedAt
    )

    private fun RoutineEntity.toDomain(): Routine = Routine(
        id = id,
        title = title,
        repeatDaysMask = repeatDaysMask,
        targetTime = targetTime,
        category = category,
        createdAt = createdAt
    )

    private fun Routine.toEntity(): RoutineEntity = RoutineEntity(
        id = id,
        title = title,
        repeatDaysMask = repeatDaysMask,
        targetTime = targetTime,
        category = category,
        createdAt = createdAt
    )

    private fun RoutineLogEntity.toDomain(): RoutineLog = RoutineLog(
        id = id,
        routineId = routineId,
        date = date,
        isCompleted = isCompleted,
        completedAt = completedAt
    )

    private fun RoutineLog.toEntity(): RoutineLogEntity = RoutineLogEntity(
        id = id,
        routineId = routineId,
        date = date,
        isCompleted = isCompleted,
        completedAt = completedAt
    )
}
