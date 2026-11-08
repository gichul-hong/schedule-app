package com.scheduleapp.test

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// --- Mock Domain Models & In-Memory Implementations for Opaque Domain Testing ---

enum class Priority { LOW, MEDIUM, HIGH }

data class ScheduleEvent(
    val id: String,
    val externalEventId: String? = null,
    val title: String,
    val startTime: Instant,
    val endTime: Instant,
    val isAllDay: Boolean = false,
    val category: String = "DEFAULT",
    val linkedCarRecordId: String? = null
)

data class TodoItem(
    val id: String,
    val title: String,
    val dueDate: LocalDate? = null,
    val priority: Priority = Priority.MEDIUM,
    val isCompleted: Boolean = false,
    val completedAt: Instant? = null
)

data class Routine(
    val id: String,
    val title: String,
    val repeatDaysMask: Int, // Bitmask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64
    val startDate: LocalDate,
    val isActive: Boolean = true
)

data class RoutineLog(
    val id: String,
    val routineId: String,
    val date: LocalDate,
    val isCompleted: Boolean
)

object RoutineStreakEngine {
    // dayOfWeek: 1 = Monday, 7 = Sunday
    fun isScheduledOn(repeatDaysMask: Int, dayOfWeek: Int): Boolean {
        val bit = 1 shl (dayOfWeek - 1)
        return (repeatDaysMask and bit) != 0
    }

    /**
     * Calculates consecutive completed scheduled days ending at or prior to [referenceDate].
     * Days not scheduled in [repeatDaysMask] do NOT break the streak.
     */
    fun calculateStreak(
        routine: Routine,
        logs: List<RoutineLog>,
        referenceDate: LocalDate
    ): Int {
        val logMap = logs.filter { it.routineId == routine.id }.associateBy { it.date }
        var streak = 0
        var currentDate = referenceDate

        // Walk backwards up to routine.startDate
        while (currentDate >= routine.startDate) {
            val dayOfWeek = currentDate.dayOfWeek.isoDayNumber
            val isScheduled = isScheduledOn(routine.repeatDaysMask, dayOfWeek)

            if (isScheduled) {
                val log = logMap[currentDate]
                if (log != null && log.isCompleted) {
                    streak++
                } else {
                    // Missed a scheduled day -> streak broken
                    break
                }
            }
            // Move one day backward
            currentDate = LocalDate.fromEpochDays(currentDate.toEpochDays() - 1)
        }
        return streak
    }
}

class InMemoryScheduleRepository {
    val events = mutableListOf<ScheduleEvent>()
    val todos = mutableListOf<TodoItem>()
    val routines = mutableListOf<Routine>()
    val routineLogs = mutableListOf<RoutineLog>()

    fun addEvent(event: ScheduleEvent) {
        require(event.endTime >= event.startTime) { "Event endTime cannot be earlier than startTime" }
        events.removeAll { it.id == event.id }
        events.add(event)
    }

    fun getEventsForRange(start: Instant, end: Instant): List<ScheduleEvent> {
        return events.filter { it.startTime <= end && it.endTime >= start }
    }

    fun addTodo(todo: TodoItem) {
        todos.removeAll { it.id == todo.id }
        todos.add(todo)
    }

    fun setTodoCompleted(id: String, completed: Boolean, timestamp: Instant?) {
        val index = todos.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = todos[index]
            todos[index] = item.copy(isCompleted = completed, completedAt = if (completed) timestamp else null)
        }
    }

    fun addRoutine(routine: Routine) {
        routines.removeAll { it.id == routine.id }
        routines.add(routine)
    }

    fun logRoutine(log: RoutineLog) {
        routineLogs.removeAll { it.routineId == log.routineId && it.date == log.date }
        routineLogs.add(log)
    }
}

class ScheduleDomainE2ETest {

    private val tz = TimeZone.UTC

    private fun instantOf(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Instant {
        return LocalDateTime(year, month, day, hour, minute).toInstant(tz)
    }

    // ==========================================
    // Tier 1: Feature Coverage (Schedule & Calendar)
    // ==========================================

    @Test
    fun testCalendar_CreateSingleDayEventWithTime() {
        val repo = InMemoryScheduleRepository()
        val start = instantOf(2026, 11, 10, 10, 0)
        val end = instantOf(2026, 11, 10, 11, 30)
        val event = ScheduleEvent("e1", title = "Team Sync", startTime = start, endTime = end, category = "WORK")
        repo.addEvent(event)

        val retrieved = repo.getEventsForRange(start, end)
        assertEquals(1, retrieved.size)
        assertEquals("Team Sync", retrieved.first().title)
        assertFalse(retrieved.first().isAllDay)
    }

    @Test
    fun testCalendar_CreateAllDayEvent() {
        val repo = InMemoryScheduleRepository()
        val start = instantOf(2026, 11, 15, 0, 0)
        val end = instantOf(2026, 11, 15, 23, 59)
        val event = ScheduleEvent("e2", title = "National Holiday", startTime = start, endTime = end, isAllDay = true)
        repo.addEvent(event)

        val retrieved = repo.getEventsForRange(start, end)
        assertEquals(1, retrieved.size)
        assertTrue(retrieved.first().isAllDay)
    }

    @Test
    fun testCalendar_QueryEventsForSpecificDay() {
        val repo = InMemoryScheduleRepository()
        repo.addEvent(ScheduleEvent("e1", title = "Day 1 Event", startTime = instantOf(2026, 11, 1, 9, 0), endTime = instantOf(2026, 11, 1, 10, 0)))
        repo.addEvent(ScheduleEvent("e2", title = "Day 2 Event", startTime = instantOf(2026, 11, 2, 9, 0), endTime = instantOf(2026, 11, 2, 10, 0)))

        val day1Results = repo.getEventsForRange(instantOf(2026, 11, 1, 0, 0), instantOf(2026, 11, 1, 23, 59))
        assertEquals(1, day1Results.size)
        assertEquals("Day 1 Event", day1Results.first().title)
    }

    @Test
    fun testCalendar_QueryEventsForMonthlyRange() {
        val repo = InMemoryScheduleRepository()
        repo.addEvent(ScheduleEvent("e1", title = "Nov Event", startTime = instantOf(2026, 11, 10, 9, 0), endTime = instantOf(2026, 11, 10, 10, 0)))
        repo.addEvent(ScheduleEvent("e2", title = "Dec Event", startTime = instantOf(2026, 12, 5, 9, 0), endTime = instantOf(2026, 12, 5, 10, 0)))

        val novResults = repo.getEventsForRange(instantOf(2026, 11, 1, 0, 0), instantOf(2026, 11, 30, 23, 59))
        assertEquals(1, novResults.size)
        assertEquals("Nov Event", novResults.first().title)
    }

    @Test
    fun testCalendar_UpdateAndDeleteEvent() {
        val repo = InMemoryScheduleRepository()
        val event = ScheduleEvent("e1", title = "Original", startTime = instantOf(2026, 11, 10, 9, 0), endTime = instantOf(2026, 11, 10, 10, 0))
        repo.addEvent(event)
        assertEquals("Original", repo.events.first().title)

        // Update
        repo.addEvent(event.copy(title = "Updated Title"))
        assertEquals(1, repo.events.size)
        assertEquals("Updated Title", repo.events.first().title)

        // Delete
        repo.events.removeAll { it.id == "e1" }
        assertTrue(repo.events.isEmpty())
    }

    // ==========================================
    // Tier 1: Feature Coverage (To-Do Lifecycle)
    // ==========================================

    @Test
    fun testTodo_CreateWithPriorityAndDueDate() {
        val repo = InMemoryScheduleRepository()
        val todo = TodoItem("t1", title = "Submit Tax", dueDate = LocalDate(2026, 11, 20), priority = Priority.HIGH)
        repo.addTodo(todo)

        assertEquals(1, repo.todos.size)
        assertEquals(Priority.HIGH, repo.todos.first().priority)
        assertFalse(repo.todos.first().isCompleted)
    }

    @Test
    fun testTodo_CompleteLifecycle_SetsCompletedAt() {
        val repo = InMemoryScheduleRepository()
        val todo = TodoItem("t1", title = "Buy Groceries")
        repo.addTodo(todo)

        val completionTime = instantOf(2026, 11, 8, 14, 30)
        repo.setTodoCompleted("t1", completed = true, timestamp = completionTime)

        val updated = repo.todos.first()
        assertTrue(updated.isCompleted)
        assertEquals(completionTime, updated.completedAt)
    }

    @Test
    fun testTodo_UncompleteLifecycle_ResetsCompletedAt() {
        val repo = InMemoryScheduleRepository()
        val todo = TodoItem("t1", title = "Clean Car", isCompleted = true, completedAt = instantOf(2026, 11, 8, 10, 0))
        repo.addTodo(todo)

        repo.setTodoCompleted("t1", completed = false, timestamp = null)
        val updated = repo.todos.first()
        assertFalse(updated.isCompleted)
        assertNull(updated.completedAt)
    }

    @Test
    fun testTodo_FilterCompletedVsPending() {
        val repo = InMemoryScheduleRepository()
        repo.addTodo(TodoItem("t1", "Task 1", isCompleted = false))
        repo.addTodo(TodoItem("t2", "Task 2", isCompleted = true))
        repo.addTodo(TodoItem("t3", "Task 3", isCompleted = false))

        val pending = repo.todos.filter { !it.isCompleted }
        val completed = repo.todos.filter { it.isCompleted }

        assertEquals(2, pending.size)
        assertEquals(1, completed.size)
    }

    @Test
    fun testTodo_SortByPriorityDescending() {
        val repo = InMemoryScheduleRepository()
        repo.addTodo(TodoItem("t1", "Low Priority", priority = Priority.LOW))
        repo.addTodo(TodoItem("t2", "High Priority", priority = Priority.HIGH))
        repo.addTodo(TodoItem("t3", "Medium Priority", priority = Priority.MEDIUM))

        val sorted = repo.todos.sortedByDescending { it.priority.ordinal }
        assertEquals("High Priority", sorted[0].title)
        assertEquals("Medium Priority", sorted[1].title)
        assertEquals("Low Priority", sorted[2].title)
    }

    // ==========================================
    // Tier 1: Feature Coverage (Routine Habit Tracker & Streaks)
    // ==========================================

    @Test
    fun testRoutine_WeekdayMaskFiltering() {
        // Mon-Fri: 1 + 2 + 4 + 8 + 16 = 31
        val weekdayMask = 31
        assertTrue(RoutineStreakEngine.isScheduledOn(weekdayMask, 1)) // Monday
        assertTrue(RoutineStreakEngine.isScheduledOn(weekdayMask, 5)) // Friday
        assertFalse(RoutineStreakEngine.isScheduledOn(weekdayMask, 6)) // Saturday
        assertFalse(RoutineStreakEngine.isScheduledOn(weekdayMask, 7)) // Sunday
    }

    @Test
    fun testRoutine_DailyRoutine_1DayStreak() {
        val routine = Routine("r1", "Drink 2L Water", repeatDaysMask = 127, startDate = LocalDate(2026, 11, 1))
        val logs = listOf(RoutineLog("l1", "r1", LocalDate(2026, 11, 8), isCompleted = true))

        val streak = RoutineStreakEngine.calculateStreak(routine, logs, referenceDate = LocalDate(2026, 11, 8))
        assertEquals(1, streak)
    }

    @Test
    fun testRoutine_DailyRoutine_MultiDayConsecutiveStreak() {
        val routine = Routine("r1", "Exercise", repeatDaysMask = 127, startDate = LocalDate(2026, 11, 1))
        val logs = listOf(
            RoutineLog("l1", "r1", LocalDate(2026, 11, 5), isCompleted = true),
            RoutineLog("l2", "r1", LocalDate(2026, 11, 6), isCompleted = true),
            RoutineLog("l3", "r1", LocalDate(2026, 11, 7), isCompleted = true),
            RoutineLog("l4", "r1", LocalDate(2026, 11, 8), isCompleted = true)
        )

        val streak = RoutineStreakEngine.calculateStreak(routine, logs, referenceDate = LocalDate(2026, 11, 8))
        assertEquals(4, streak)
    }

    @Test
    fun testRoutine_ZeroStreak_WhenNoLogsExist() {
        val routine = Routine("r1", "Read Book", repeatDaysMask = 127, startDate = LocalDate(2026, 11, 1))
        val streak = RoutineStreakEngine.calculateStreak(routine, emptyList(), referenceDate = LocalDate(2026, 11, 8))
        assertEquals(0, streak)
    }

    @Test
    fun testRoutine_InactiveRoutine_MaintainsDefinition() {
        val routine = Routine("r1", "Old Habit", repeatDaysMask = 127, startDate = LocalDate(2026, 1, 1), isActive = false)
        assertFalse(routine.isActive)
    }

    // ==========================================
    // Tier 2: Boundary & Corner Cases
    // ==========================================

    @Test
    fun testBoundary_EventEndBeforeStart_ThrowsException() {
        val repo = InMemoryScheduleRepository()
        val start = instantOf(2026, 11, 10, 12, 0)
        val end = instantOf(2026, 11, 10, 11, 0) // Invalid

        assertFailsWith<IllegalArgumentException> {
            repo.addEvent(ScheduleEvent("e_invalid", title = "Invalid", startTime = start, endTime = end))
        }
    }

    @Test
    fun testBoundary_ZeroDurationEvent_Allowed() {
        val repo = InMemoryScheduleRepository()
        val instant = instantOf(2026, 11, 10, 12, 0)
        val event = ScheduleEvent("e_instant", title = "Checkpoint", startTime = instant, endTime = instant)
        repo.addEvent(event)

        val result = repo.getEventsForRange(instant, instant)
        assertEquals(1, result.size)
    }

    @Test
    fun testBoundary_MultiDayAllDayEvent_SpansMonthBoundary() {
        val repo = InMemoryScheduleRepository()
        val start = instantOf(2026, 10, 31, 0, 0)
        val end = instantOf(2026, 11, 2, 23, 59)
        val event = ScheduleEvent("e_trip", title = "Workshop Trip", startTime = start, endTime = end, isAllDay = true)
        repo.addEvent(event)

        // Querying Nov 1 should include this multi-day event
        val nov1Start = instantOf(2026, 11, 1, 0, 0)
        val nov1End = instantOf(2026, 11, 1, 23, 59)
        val result = repo.getEventsForRange(nov1Start, nov1End)
        assertEquals(1, result.size)
        assertEquals("Workshop Trip", result.first().title)
    }

    @Test
    fun testBoundary_EmptyScheduleRange_ReturnsEmptyList() {
        val repo = InMemoryScheduleRepository()
        val result = repo.getEventsForRange(instantOf(2026, 1, 1), instantOf(2026, 1, 2))
        assertTrue(result.isEmpty())
    }

    @Test
    fun testBoundary_RoutineStreak_PreservedAcrossNonScheduledWeekend() {
        // Mon-Fri routine (mask=31)
        // 2026-11-06 = Friday (Day 5)
        // 2026-11-07 = Saturday (Day 6 - non-scheduled)
        // 2026-11-08 = Sunday (Day 7 - non-scheduled)
        // 2026-11-09 = Monday (Day 1)
        val routine = Routine("r_work", "Work Log", repeatDaysMask = 31, startDate = LocalDate(2026, 11, 1))
        val logs = listOf(
            RoutineLog("l1", "r_work", LocalDate(2026, 11, 6), isCompleted = true), // Fri
            // Sat & Sun have NO logs because they are not scheduled
            RoutineLog("l2", "r_work", LocalDate(2026, 11, 9), isCompleted = true)  // Mon
        )

        val streak = RoutineStreakEngine.calculateStreak(routine, logs, referenceDate = LocalDate(2026, 11, 9))
        // Should be 2 consecutive scheduled days! Weekend did NOT break streak
        assertEquals(2, streak)
    }

    @Test
    fun testBoundary_RoutineStreak_BrokenOnMissedScheduledDay() {
        // Daily routine (mask=127)
        val routine = Routine("r1", "Daily Habit", repeatDaysMask = 127, startDate = LocalDate(2026, 11, 1))
        val logs = listOf(
            RoutineLog("l1", "r1", LocalDate(2026, 11, 5), isCompleted = true),
            // Missed 2026-11-06!
            RoutineLog("l3", "r1", LocalDate(2026, 11, 7), isCompleted = true),
            RoutineLog("l4", "r1", LocalDate(2026, 11, 8), isCompleted = true)
        )

        val streak = RoutineStreakEngine.calculateStreak(routine, logs, referenceDate = LocalDate(2026, 11, 8))
        // Only Nov 7 and Nov 8 are in the current unbroken streak
        assertEquals(2, streak)
    }

    @Test
    fun testBoundary_RoutineStreak_AcrossYearTransition() {
        // Daily routine across Dec 31, 2025 -> Jan 1, 2026
        val routine = Routine("r_year", "Meditation", repeatDaysMask = 127, startDate = LocalDate(2025, 12, 1))
        val logs = listOf(
            RoutineLog("l1", "r_year", LocalDate(2025, 12, 30), isCompleted = true),
            RoutineLog("l2", "r_year", LocalDate(2025, 12, 31), isCompleted = true),
            RoutineLog("l3", "r_year", LocalDate(2026, 1, 1), isCompleted = true),
            RoutineLog("l4", "r_year", LocalDate(2026, 1, 2), isCompleted = true)
        )

        val streak = RoutineStreakEngine.calculateStreak(routine, logs, referenceDate = LocalDate(2026, 1, 2))
        assertEquals(4, streak)
    }
}
