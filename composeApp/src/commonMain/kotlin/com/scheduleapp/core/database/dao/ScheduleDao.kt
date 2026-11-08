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

    @Query("SELECT * FROM todo_items WHERE id = :id")
    suspend fun getTodoById(id: Long): TodoItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoItemEntity): Long

    @Update
    suspend fun updateTodo(todo: TodoItemEntity)

    @Delete
    suspend fun deleteTodo(todo: TodoItemEntity)

    // Routines
    @Query("SELECT * FROM routines ORDER BY createdAt ASC")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutineById(id: Long): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Delete
    suspend fun deleteRoutine(routine: RoutineEntity)

    // Routine Logs
    @Query("SELECT * FROM routine_logs")
    fun getAllRoutineLogs(): Flow<List<RoutineLogEntity>>

    @Query("SELECT * FROM routine_logs WHERE date = :date")
    fun getRoutineLogsForDate(date: String): Flow<List<RoutineLogEntity>>

    @Query("SELECT * FROM routine_logs WHERE routineId = :routineId")
    fun getRoutineLogsForRoutine(routineId: Long): Flow<List<RoutineLogEntity>>

    @Query("SELECT * FROM routine_logs WHERE routineId = :routineId AND date = :date LIMIT 1")
    suspend fun getRoutineLog(routineId: Long, date: String): RoutineLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineLog(log: RoutineLogEntity): Long

    @Update
    suspend fun updateRoutineLog(log: RoutineLogEntity)

    @Delete
    suspend fun deleteRoutineLog(log: RoutineLogEntity)

    // Bulk Operations for Backup & Restore
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<ScheduleEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodos(todos: List<TodoItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutines(routines: List<RoutineEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineLogs(logs: List<RoutineLogEntity>)

    @Query("DELETE FROM schedule_events")
    suspend fun clearAllEvents()

    @Query("DELETE FROM todo_items")
    suspend fun clearAllTodos()

    @Query("DELETE FROM routine_logs")
    suspend fun clearAllRoutineLogs()

    @Query("DELETE FROM routines")
    suspend fun clearAllRoutines()
}
