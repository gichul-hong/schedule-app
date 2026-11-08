package com.scheduleapp.domain.carbook

import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.carbook.engine.MaintenanceWearEngine
import com.scheduleapp.domain.carbook.model.MaintenanceItem
import com.scheduleapp.domain.carbook.model.MaintenancePreset
import com.scheduleapp.domain.carbook.model.MaintenanceStatus
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MaintenanceWearEngineTest {

    @Test
    fun testWearCalculation_DistanceDominant_TriggersWarning() {
        val lastDate = LocalDate(2026, 1, 1)
        val lastDateMillis = DateTimeUtils.startOfDayMillis(lastDate)
        val nextDueDateMillis = DateTimeUtils.startOfDayMillis(LocalDate(2027, 1, 1))

        val item = MaintenanceItem(
            id = 1L,
            vehicleId = 1L,
            partName = "엔진오일",
            cycleKm = 10000.0,
            cycleMonths = 12,
            lastReplacedDate = lastDateMillis,
            lastReplacedOdometer = 40000.0,
            nextDueDate = nextDueDateMillis
        )

        // Driven 8,500 km in 3 months: Distance wear = 85%, Time wear = 25% -> Final wear = 85% (WARNING)
        val testDate = LocalDate(2026, 4, 1)
        val (wear, status) = MaintenanceWearEngine.calculateWear(item, 48500.0, testDate)

        assertEquals(85.0, wear)
        assertEquals(MaintenanceStatus.WARNING, status)
    }

    @Test
    fun testWearCalculation_TimeDominant_TriggersOverdue() {
        val lastDate = LocalDate(2025, 6, 1)
        val lastDateMillis = DateTimeUtils.startOfDayMillis(lastDate)
        val nextDueDateMillis = DateTimeUtils.startOfDayMillis(LocalDate(2026, 6, 1))

        val item = MaintenanceItem(
            id = 2L,
            vehicleId = 1L,
            partName = "에어컨 필터",
            cycleKm = 10000.0,
            cycleMonths = 6,
            lastReplacedDate = lastDateMillis,
            lastReplacedOdometer = 50000.0,
            nextDueDate = nextDueDateMillis
        )

        // Driven only 1,000 km in 7 months (Time wear ~ 116.7% > 100%)
        val testDate = LocalDate(2026, 1, 1)
        val (wear, status) = MaintenanceWearEngine.calculateWear(item, 51000.0, testDate)

        assertTrue(wear >= 100.0)
        assertEquals(MaintenanceStatus.OVERDUE, status)
    }

    @Test
    fun testWearCalculation_SingleMetric_DistanceOnly() {
        val item = MaintenanceItem(
            id = 3L,
            vehicleId = 1L,
            partName = "브레이크 패드",
            cycleKm = 30000.0,
            cycleMonths = 0, // Time disabled
            lastReplacedDate = DateTimeUtils.startOfDayMillis(LocalDate(2020, 1, 1)),
            lastReplacedOdometer = 10000.0,
            nextDueDate = 0L
        )

        // Driven 15,000 km -> 50% wear, Status OK despite 6 years elapsed
        val testDate = LocalDate(2026, 1, 1)
        val (wear, status) = MaintenanceWearEngine.calculateWear(item, 25000.0, testDate)

        assertEquals(50.0, wear)
        assertEquals(MaintenanceStatus.OK, status)
    }

    @Test
    fun testWearCalculation_SingleMetric_TimeOnly() {
        val lastDate = LocalDate(2026, 1, 1)
        val item = MaintenanceItem(
            id = 4L,
            vehicleId = 1L,
            partName = "와이퍼 블레이드",
            cycleKm = 0.0, // Distance disabled
            cycleMonths = 6,
            lastReplacedDate = DateTimeUtils.startOfDayMillis(lastDate),
            lastReplacedOdometer = 50000.0,
            nextDueDate = 0L
        )

        // Driven 100,000 km immediately, but only 3 months elapsed -> 50% wear
        val testDate = LocalDate(2026, 4, 1)
        val (wear, status) = MaintenanceWearEngine.calculateWear(item, 150000.0, testDate)

        assertEquals(50.0, wear)
        assertEquals(MaintenanceStatus.OK, status)
    }

    @Test
    fun testDetailedWear_RemainingKmAndDays() {
        val lastDate = LocalDate(2026, 1, 1)
        val lastDateMillis = DateTimeUtils.startOfDayMillis(lastDate)
        val nextDueDate = LocalDate(2026, 7, 1)
        val nextDueDateMillis = DateTimeUtils.startOfDayMillis(nextDueDate)

        val item = MaintenanceItem(
            id = 5L,
            vehicleId = 1L,
            partName = "엔진오일",
            cycleKm = 10000.0,
            cycleMonths = 6,
            lastReplacedDate = lastDateMillis,
            lastReplacedOdometer = 20000.0,
            nextDueDate = nextDueDateMillis
        )

        val currentDateMillis = DateTimeUtils.startOfDayMillis(LocalDate(2026, 6, 1))
        val result = MaintenanceWearEngine.calculateDetailedWear(
            item = item,
            currentOdometer = 27000.0,
            currentDateMillis = currentDateMillis
        )

        assertNotNull(result.remainingKm)
        assertEquals(3000.0, result.remainingKm!!)
        assertEquals(30L, result.remainingDays)
        assertTrue(result.wearPercentage in 80.0..85.0)
        assertEquals(MaintenanceStatus.WARNING, result.status)
    }

    @Test
    fun testDateUtilities_AddMonthsAndNextDueDate() {
        // Month end rollover
        val date1 = LocalDate(2025, 1, 31)
        val plus1Mo = MaintenanceWearEngine.addMonths(date1, 1)
        assertEquals(LocalDate(2025, 2, 28), plus1Mo) // Non-leap year clamped to 28

        // Leap year
        val date2 = LocalDate(2024, 1, 31)
        val plus1MoLeap = MaintenanceWearEngine.addMonths(date2, 1)
        assertEquals(LocalDate(2024, 2, 29), plus1MoLeap) // Leap year clamped to 29

        // 12 months next due date
        val jan1Millis = DateTimeUtils.startOfDayMillis(LocalDate(2026, 1, 1))
        val nextDueMillis = MaintenanceWearEngine.calculateNextDueDate(jan1Millis, 12)
        val nextDueDate = DateTimeUtils.localDateFromMillis(nextDueMillis)
        assertEquals(LocalDate(2027, 1, 1), nextDueDate)
    }

    @Test
    fun testPresets_Integrity() {
        val presets = MaintenancePreset.DEFAULT_PRESETS
        assertEquals(5, presets.size)

        val engineOil = presets.find { it.partName.contains("엔진오일") }
        assertNotNull(engineOil)
        assertEquals(10000.0, engineOil.cycleKm)
        assertEquals(12, engineOil.cycleMonths)

        val acFilter = presets.find { it.partName.contains("에어컨") }
        assertNotNull(acFilter)
        assertEquals(10000.0, acFilter.cycleKm)
        assertEquals(6, acFilter.cycleMonths)

        val brakePads = presets.find { it.partName.contains("브레이크") }
        assertNotNull(brakePads)
        assertEquals(30000.0, brakePads.cycleKm)
        assertEquals(24, brakePads.cycleMonths)

        val battery = presets.find { it.partName.contains("배터리") }
        assertNotNull(battery)
        assertEquals(50000.0, battery.cycleKm)
        assertEquals(36, battery.cycleMonths)
    }
}
