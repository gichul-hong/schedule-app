package com.scheduleapp.core.database.dao

import androidx.room.*
import com.scheduleapp.core.database.entity.RoutineEntity
import com.scheduleapp.core.database.entity.RoutineLogEntity
import com.scheduleapp.core.database.entity.ScheduleEventEntity
import com.scheduleapp.core.database.entity.TodoItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    // Schedule Events
    @Query("SELECT * FROM schedule_events ORDER BY startTime ASC")
    fun getAllEvents(): Flow<List<ScheduleEventEntity>>

    @Query("SELECT * FROM schedule_events WHERE startTime >= :start AND endTime <= :end ORDER BY startTime ASC")
    fun getEventsInRange(start: Long, end: Long): Flow<List<ScheduleEventEntity>>

    @Query("SELECT * FROM schedule_events WHERE id = :id")
    suspend fun getEventById(id: Long): ScheduleEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ScheduleEventEntity): Long

    @Update
    suspend fun updateEvent(event: ScheduleEventEntity)

    @Delete
    suspend fun deleteEvent(event: ScheduleEventEntity)

    // Todo Items
    @Query("SELECT * FROM todo_items ORDER BY dueDate ASC, priority DESC")
    fun getAllTodos(): Flow<List<TodoItemEntity>>

    @Query("SELECT * FROM todo_items WHERE dueDate >= :start AND dueDate <= :end ORDER BY priority DESC")
    fun getTodosByDate(start: Long, end: Long): Flow<List<TodoItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoItemEntity): Long

    @Update
    suspend fun updateTodo(todo: TodoItemEntity)

    @Delete
    suspend fun deleteTodo(todo: TodoItemEntity)

    // Routines
    @Query("SELECT * FROM routines ORDER BY createdAt ASC")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Delete
    suspend fun deleteRoutine(routine: RoutineEntity)

    // Routine Logs
    @Query("SELECT * FROM routine_logs WHERE date = :date")
    fun getRoutineLogsForDate(date: String): Flow<List<RoutineLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineLog(log: RoutineLogEntity): Long

    @Update
    suspend fun updateRoutineLog(log: RoutineLogEntity)

    @Delete
    suspend fun deleteRoutineLog(log: RoutineLogEntity)
}
