package com.scheduleapp.test

import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.calendar.engine.RoutineStreakEngine
import com.scheduleapp.domain.calendar.model.Priority
import com.scheduleapp.domain.calendar.model.Routine
import com.scheduleapp.domain.calendar.model.RoutineLog
import com.scheduleapp.domain.calendar.model.ScheduleEvent
import com.scheduleapp.domain.calendar.model.TodoItem
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduleDomainUnitTest {

    @Test
    fun testPriorityEnumMapping() {
        assertEquals(Priority.LOW, Priority.fromInt(0))
        assertEquals(Priority.MEDIUM, Priority.fromInt(1))
        assertEquals(Priority.HIGH, Priority.fromInt(2))
        assertEquals(Priority.URGENT, Priority.fromInt(3))
        assertEquals(Priority.MEDIUM, Priority.fromInt(99)) // Fallback default
    }

    @Test
    fun testScheduleEvent_Validation() {
        // Valid event
        val validEvent = ScheduleEvent(
            title = "Valid Event",
            startTime = 1000L,
            endTime = 2000L
        )
        assertEquals("Valid Event", validEvent.title)

        // Zero duration event is valid
        val zeroDuration = ScheduleEvent(
            title = "Zero Duration",
            startTime = 1000L,
            endTime = 1000L
        )
        assertEquals(1000L, zeroDuration.endTime)

        // Invalid: endTime < startTime
        assertFailsWith<IllegalArgumentException> {
            ScheduleEvent(
                title = "Invalid Event",
                startTime = 2000L,
                endTime = 1000L
            )
        }
    }

    @Test
    fun testRoutineStreakEngine_WeekdayMask() {
        // Mon-Fri: 1 + 2 + 4 + 8 + 16 = 31
        val weekdayMask = 31
        assertTrue(RoutineStreakEngine.isScheduledOn(weekdayMask, 1)) // Mon
        assertTrue(RoutineStreakEngine.isScheduledOn(weekdayMask, 2)) // Tue
        assertTrue(RoutineStreakEngine.isScheduledOn(weekdayMask, 3)) // Wed
        assertTrue(RoutineStreakEngine.isScheduledOn(weekdayMask, 4)) // Thu
        assertTrue(RoutineStreakEngine.isScheduledOn(weekdayMask, 5)) // Fri
        assertFalse(RoutineStreakEngine.isScheduledOn(weekdayMask, 6)) // Sat
        assertFalse(RoutineStreakEngine.isScheduledOn(weekdayMask, 7)) // Sun

        // Weekend: 32 + 64 = 96
        val weekendMask = 96
        assertFalse(RoutineStreakEngine.isScheduledOn(weekendMask, 1))
        assertFalse(RoutineStreakEngine.isScheduledOn(weekendMask, 5))
        assertTrue(RoutineStreakEngine.isScheduledOn(weekendMask, 6))
        assertTrue(RoutineStreakEngine.isScheduledOn(weekendMask, 7))
    }

    @Test
    fun testRoutineStreakEngine_ConsecutiveStreak() {
        val routine = Routine(id = 1L, title = "Exercise", repeatDaysMask = 127)
        val logs = listOf(
            RoutineLog(id = 1L, routineId = 1L, date = "2026-11-05", isCompleted = true),
            RoutineLog(id = 2L, routineId = 1L, date = "2026-11-06", isCompleted = true),
            RoutineLog(id = 3L, routineId = 1L, date = "2026-11-07", isCompleted = true),
            RoutineLog(id = 4L, routineId = 1L, date = "2026-11-08", isCompleted = true)
        )

        val streak = RoutineStreakEngine.calculateStreak(
            routine = routine,
            logs = logs,
            referenceDate = LocalDate(2026, 11, 8)
        )
        assertEquals(4, streak)
    }

    @Test
    fun testRoutineStreakEngine_NonScheduledDaysDoNotBreakStreak() {
        // Mon-Fri routine (mask 31)
        // 2026-11-06: Friday
        // 2026-11-07: Saturday (not scheduled)
        // 2026-11-08: Sunday (not scheduled)
        // 2026-11-09: Monday
        val routine = Routine(id = 1L, title = "Work Log", repeatDaysMask = 31)
        val logs = listOf(
            RoutineLog(id = 1L, routineId = 1L, date = "2026-11-06", isCompleted = true),
            // Weekend has no logs
            RoutineLog(id = 2L, routineId = 1L, date = "2026-11-09", isCompleted = true)
        )

        val streak = RoutineStreakEngine.calculateStreak(
            routine = routine,
            logs = logs,
            referenceDate = LocalDate(2026, 11, 9)
        )
        // 2 scheduled days completed consecutively across weekend
        assertEquals(2, streak)
    }

    @Test
    fun testRoutineStreakEngine_MissedScheduledDayBreaksStreak() {
        val routine = Routine(id = 1L, title = "Daily Reading", repeatDaysMask = 127)
        val logs = listOf(
            RoutineLog(id = 1L, routineId = 1L, date = "2026-11-05", isCompleted = true),
            // 2026-11-06 missed!
            RoutineLog(id = 3L, routineId = 1L, date = "2026-11-07", isCompleted = true),
            RoutineLog(id = 4L, routineId = 1L, date = "2026-11-08", isCompleted = true)
        )

        val streak = RoutineStreakEngine.calculateStreak(
            routine = routine,
            logs = logs,
            referenceDate = LocalDate(2026, 11, 8)
        )
        assertEquals(2, streak)
    }

    @Test
    fun testRoutineStreakEngine_AllowTodayPending() {
        val routine = Routine(id = 1L, title = "Morning Run", repeatDaysMask = 127)
        val logs = listOf(
            RoutineLog(id = 1L, routineId = 1L, date = "2026-11-07", isCompleted = true)
            // Today (2026-11-08) is still pending
        )

        // Without allowTodayPending -> streak is 0 because today is not completed
        val strictStreak = RoutineStreakEngine.calculateStreak(
            routine = routine,
            logs = logs,
            referenceDate = LocalDate(2026, 11, 8),
            allowTodayPending = false
        )
        assertEquals(0, strictStreak)

        // With allowTodayPending -> streak preserves yesterday's 1-day streak
        val pendingStreak = RoutineStreakEngine.calculateStreak(
            routine = routine,
            logs = logs,
            referenceDate = LocalDate(2026, 11, 8),
            allowTodayPending = true
        )
        assertEquals(1, pendingStreak)
    }

    @Test
    fun testRoutineStreakEngine_CompletionRate() {
        val routines = listOf(
            Routine(id = 1L, title = "R1", repeatDaysMask = 127),
            Routine(id = 2L, title = "R2", repeatDaysMask = 127),
            Routine(id = 3L, title = "R3", repeatDaysMask = 127)
        )
        val today = LocalDate(2026, 11, 8)
        val logs = listOf(
            RoutineLog(id = 1L, routineId = 1L, date = "2026-11-08", isCompleted = true),
            RoutineLog(id = 2L, routineId = 2L, date = "2026-11-08", isCompleted = true)
        )

        val rate = RoutineStreakEngine.calculateCompletionRate(routines, logs, today)
        assertEquals(2f / 3f, rate)
    }

    @Test
    fun testDateTimeUtils_FormattingAndParsing() {
        val date = LocalDate(2026, 11, 8)
        val formatted = DateTimeUtils.formatDate(date)
        assertEquals("2026-11-08", formatted)

        val parsed = DateTimeUtils.parseDate(formatted)
        assertEquals(date, parsed)
    }
}
