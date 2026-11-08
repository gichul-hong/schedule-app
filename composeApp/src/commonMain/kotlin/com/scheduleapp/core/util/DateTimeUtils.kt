package com.scheduleapp.core.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

object DateTimeUtils {

    val defaultTimeZone: TimeZone = TimeZone.currentSystemDefault()

    fun today(): LocalDate {
        return Clock.System.now().toLocalDateTime(defaultTimeZone).date
    }

    fun nowMillis(): Long {
        return Clock.System.now().toEpochMilliseconds()
    }

    fun localDateFromMillis(millis: Long, timeZone: TimeZone = defaultTimeZone): LocalDate {
        return Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone).date
    }

    fun startOfDayMillis(date: LocalDate, timeZone: TimeZone = defaultTimeZone): Long {
        return date.atTime(0, 0, 0, 0).toInstant(timeZone).toEpochMilliseconds()
    }

    fun endOfDayMillis(date: LocalDate, timeZone: TimeZone = defaultTimeZone): Long {
        return date.atTime(23, 59, 59, 999_999_999).toInstant(timeZone).toEpochMilliseconds()
    }

    fun startOfMonth(year: Int, month: Int): LocalDate {
        return LocalDate(year, month, 1)
    }

    fun endOfMonth(year: Int, month: Int): LocalDate {
        val nextMonth = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
        return LocalDate.fromEpochDays(nextMonth.toEpochDays() - 1)
    }

    fun formatDate(date: LocalDate): String {
        val y = date.year.toString().padStart(4, '0')
        val m = date.monthNumber.toString().padStart(2, '0')
        val d = date.dayOfMonth.toString().padStart(2, '0')
        return "$y-$m-$d"
    }

    fun parseDate(dateStr: String): LocalDate {
        return LocalDate.parse(dateStr)
    }

    fun formatTime(millis: Long, timeZone: TimeZone = defaultTimeZone): String {
        val ldt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone)
        val h = ldt.hour.toString().padStart(2, '0')
        val min = ldt.minute.toString().padStart(2, '0')
        return "$h:$min"
    }

    fun formatDateTime(millis: Long, timeZone: TimeZone = defaultTimeZone): String {
        val ldt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone)
        val y = ldt.year.toString().padStart(4, '0')
        val m = ldt.monthNumber.toString().padStart(2, '0')
        val d = ldt.dayOfMonth.toString().padStart(2, '0')
        val h = ldt.hour.toString().padStart(2, '0')
        val min = ldt.minute.toString().padStart(2, '0')
        return "$y.$m.$d $h:$min"
    }
}
