package com.scheduleapp.domain.calendar.engine

import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.calendar.model.Routine
import com.scheduleapp.domain.calendar.model.RoutineLog
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime

object RoutineStreakEngine {

    /**
     * Checks if the given day of the week is scheduled in repeatDaysMask.
     * @param repeatDaysMask Bitmask where Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64
     * @param dayOfWeek 1 = Monday, ..., 7 = Sunday (matching kotlinx.datetime isoDayNumber)
     */
    fun isScheduledOn(repeatDaysMask: Int, dayOfWeek: Int): Boolean {
        require(dayOfWeek in 1..7) { "dayOfWeek must be between 1 (Monday) and 7 (Sunday)" }
        val bit = 1 shl (dayOfWeek - 1)
        return (repeatDaysMask and bit) != 0
    }

    /**
     * Checks if the routine is scheduled on [date].
     */
    fun isScheduledOnDate(repeatDaysMask: Int, date: LocalDate): Boolean {
        return isScheduledOn(repeatDaysMask, date.dayOfWeek.isoDayNumber)
    }

    /**
     * Returns list of scheduled days (1 = Monday, 7 = Sunday).
     */
    fun getScheduledDaysOfWeek(repeatDaysMask: Int): List<Int> {
        return (1..7).filter { isScheduledOn(repeatDaysMask, it) }
    }

    /**
     * Calculates consecutive completed scheduled days ending at or prior to [referenceDate].
     * Days not scheduled in [repeatDaysMask] do NOT break the streak.
     *
     * @param allowTodayPending If true and [referenceDate] is not yet completed, does not break streak
     *                          so users viewing today aren't penalized before the day ends.
     */
    fun calculateStreak(
        repeatDaysMask: Int,
        startDate: LocalDate,
        logs: List<RoutineLog>,
        routineId: Long,
        referenceDate: LocalDate,
        allowTodayPending: Boolean = false
    ): Int {
        val logMap = logs.filter { it.routineId == routineId }.associateBy { it.date }
        var streak = 0
        var currentDate = referenceDate

        while (currentDate >= startDate) {
            val dayOfWeek = currentDate.dayOfWeek.isoDayNumber
            val isScheduled = isScheduledOn(repeatDaysMask, dayOfWeek)

            if (isScheduled) {
                val dateStr = currentDate.toString()
                val log = logMap[dateStr]

                if (log != null && log.isCompleted) {
                    streak++
                } else {
                    if (currentDate == referenceDate && allowTodayPending) {
                        // Pending today does not break yesterday's streak
                    } else {
                        // Missed a scheduled day -> streak broken
                        break
                    }
                }
            }
            // Move one day backward
            currentDate = LocalDate.fromEpochDays(currentDate.toEpochDays() - 1)
        }
        return streak
    }

    fun calculateStreak(
        routine: Routine,
        logs: List<RoutineLog>,
        referenceDate: LocalDate,
        allowTodayPending: Boolean = false
    ): Int {
        val startDate = if (routine.createdAt > 0) {
            try {
                Instant.fromEpochMilliseconds(routine.createdAt)
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date
            } catch (e: Exception) {
                LocalDate(2020, 1, 1)
            }
        } else {
            LocalDate(2020, 1, 1)
        }
        return calculateStreak(
            repeatDaysMask = routine.repeatDaysMask,
            startDate = startDate,
            logs = logs,
            routineId = routine.id,
            referenceDate = referenceDate,
            allowTodayPending = allowTodayPending
        )
    }

    /**
     * Calculates completion rate (0.0 to 1.0) for scheduled routines on [date].
     */
    fun calculateCompletionRate(
        routines: List<Routine>,
        logsForDate: List<RoutineLog>,
        date: LocalDate = DateTimeUtils.today()
    ): Float {
        val scheduledRoutines = routines.filter { isScheduledOnDate(it.repeatDaysMask, date) }
        if (scheduledRoutines.isEmpty()) return 0f

        val completedCount = scheduledRoutines.count { r ->
            logsForDate.any { it.routineId == r.id && it.isCompleted }
        }
        return completedCount.toFloat() / scheduledRoutines.size.toFloat()
    }
}
