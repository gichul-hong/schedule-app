package com.scheduleapp.domain.carbook.engine

import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.carbook.model.MaintenanceItem
import com.scheduleapp.domain.carbook.model.MaintenanceStatus
import kotlinx.datetime.LocalDate
import kotlin.math.round

data class MaintenanceWearResult(
    val wearPercentage: Double,
    val status: MaintenanceStatus,
    val distanceWearPercentage: Double,
    val timeWearPercentage: Double,
    val remainingKm: Double?,
    val remainingDays: Long
)

object MaintenanceWearEngine {

    /**
     * Calculates dual-metric wear percentage and maintenance status.
     * Distance wear: (currentOdo - lastOdo) / cycleKm * 100% (if cycleKm > 0)
     * Time wear: monthsBetween(lastDate, currentDate) / cycleMonths * 100% (if cycleMonths > 0)
     * Final wear = max(distanceWear, timeWear)
     */
    fun calculateWear(
        item: MaintenanceItem,
        currentOdometer: Double,
        currentDate: LocalDate
    ): Pair<Double, MaintenanceStatus> {
        val lastDate = DateTimeUtils.localDateFromMillis(item.lastReplacedDate)

        val distanceWear = if (item.cycleKm > 0.0) {
            val deltaKm = (currentOdometer - item.lastReplacedOdometer).coerceAtLeast(0.0)
            (deltaKm / item.cycleKm) * 100.0
        } else {
            0.0
        }

        val timeWear = if (item.cycleMonths > 0) {
            val monthsElapsed = monthsBetween(lastDate, currentDate).coerceAtLeast(0.0)
            (monthsElapsed / item.cycleMonths.toDouble()) * 100.0
        } else {
            0.0
        }

        val maxWear = round(maxOf(distanceWear, timeWear) * 10.0) / 10.0
        val status = resolveStatus(maxWear)

        return Pair(maxWear, status)
    }

    /**
     * Detailed wear computation returning separate distance/time wear,
     * remaining distance (km) and remaining days until next due date.
     */
    fun calculateDetailedWear(
        item: MaintenanceItem,
        currentOdometer: Double,
        currentDateMillis: Long = DateTimeUtils.nowMillis()
    ): MaintenanceWearResult {
        val currentDate = DateTimeUtils.localDateFromMillis(currentDateMillis)
        val lastDate = DateTimeUtils.localDateFromMillis(item.lastReplacedDate)

        val distanceWear = if (item.cycleKm > 0.0) {
            val deltaKm = (currentOdometer - item.lastReplacedOdometer).coerceAtLeast(0.0)
            round((deltaKm / item.cycleKm) * 1000.0) / 10.0
        } else {
            0.0
        }

        val timeWear = if (item.cycleMonths > 0) {
            val monthsElapsed = monthsBetween(lastDate, currentDate).coerceAtLeast(0.0)
            round((monthsElapsed / item.cycleMonths.toDouble()) * 1000.0) / 10.0
        } else {
            0.0
        }

        val maxWear = round(maxOf(distanceWear, timeWear) * 10.0) / 10.0
        val status = resolveStatus(maxWear)

        val remainingKm = if (item.cycleKm > 0.0) {
            val driven = (currentOdometer - item.lastReplacedOdometer).coerceAtLeast(0.0)
            item.cycleKm - driven
        } else {
            null
        }

        val remainingDays = ((item.nextDueDate - currentDateMillis) / (1000L * 60 * 60 * 24))

        return MaintenanceWearResult(
            wearPercentage = maxWear,
            status = status,
            distanceWearPercentage = distanceWear,
            timeWearPercentage = timeWear,
            remainingKm = remainingKm,
            remainingDays = remainingDays
        )
    }

    private fun resolveStatus(wearPercentage: Double): MaintenanceStatus {
        return when {
            wearPercentage >= 100.0 -> MaintenanceStatus.OVERDUE
            wearPercentage >= 80.0 -> MaintenanceStatus.WARNING
            else -> MaintenanceStatus.OK
        }
    }

    fun monthsBetween(start: LocalDate, end: LocalDate): Double {
        val yearDiff = end.year - start.year
        val monthDiff = end.monthNumber - start.monthNumber
        val dayDiff = (end.dayOfMonth - start.dayOfMonth) / 30.0
        return (yearDiff * 12 + monthDiff + dayDiff).coerceAtLeast(0.0)
    }

    fun addMonths(date: LocalDate, months: Int): LocalDate {
        if (months <= 0) return date
        val totalMonths = date.monthNumber - 1 + months
        val newYear = date.year + (totalMonths / 12)
        val newMonth = (totalMonths % 12) + 1
        val maxDays = daysInMonth(newYear, newMonth)
        val newDay = minOf(date.dayOfMonth, maxDays)
        return LocalDate(newYear, newMonth, newDay)
    }

    fun daysInMonth(year: Int, month: Int): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 30
        }
    }

    fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    /**
     * Calculates the next due date in epoch milliseconds based on last replaced date and cycle months.
     * If cycleMonths is 0, defaults to 12 months as calendar reminder fallback.
     */
    fun calculateNextDueDate(lastReplacedDateMillis: Long, cycleMonths: Int): Long {
        val lastDate = DateTimeUtils.localDateFromMillis(lastReplacedDateMillis)
        val monthsToAdd = if (cycleMonths > 0) cycleMonths else 12
        val nextDate = addMonths(lastDate, monthsToAdd)
        return DateTimeUtils.startOfDayMillis(nextDate)
    }
}
