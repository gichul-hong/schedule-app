package com.scheduleapp.test

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MaintenanceSyncE2ETest {

    private val tz = TimeZone.UTC

    private fun dateToInstant(date: LocalDate): Instant {
        return LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, 9, 0).toInstant(tz)
    }

    // ==========================================
    // Tier 3: Pairwise Cross-Feature Interactions
    // ==========================================

    @Test
    fun testSync_LogMaintenanceExpense_ResetsWear_ReschedulesCalendarEvent() {
        val scheduleRepo = InMemoryScheduleRepository()
        val vehicle = Vehicle("v1", "Ioniq 5", "EV", FuelType.ELECTRIC, 40000.0, 50000.0)

        // 1. Initial Maintenance Item: Next due at 2026-06-01
        var item = MaintenanceItem(
            id = "m_oil",
            vehicleId = "v1",
            partName = "Reduction Gear Oil",
            cycleKm = 20000.0,
            cycleMonths = 12,
            lastReplacedDate = LocalDate(2025, 6, 1),
            lastReplacedOdometer = 40000.0,
            nextDueDate = LocalDate(2026, 6, 1),
            scheduledEventId = "event_oil_1"
        )
        // Existing Calendar event
        scheduleRepo.addEvent(
            ScheduleEvent(
                id = item.scheduledEventId!!,
                title = "차량 정비 알림: Reduction Gear Oil",
                startTime = dateToInstant(item.nextDueDate),
                endTime = dateToInstant(item.nextDueDate),
                category = "CAR_MAINTENANCE",
                linkedCarRecordId = item.id
            )
        )
        assertEquals(1, scheduleRepo.events.size)

        // 2. User services car at 50,500 km on 2026-06-05
        val serviceDate = LocalDate(2026, 6, 5)
        val serviceOdometer = 50500.0
        val serviceExpense = CarExpenseLog("exp_1", "v1", serviceDate, ExpenseType.MAINTENANCE, 150000.0, serviceOdometer, "Oil replaced")

        // Update vehicle odometer
        vehicle.currentOdometer = serviceOdometer

        // Recalculate maintenance baseline & next due date (+12 months -> 2027-06-05)
        val newDueDate = LocalDate(2027, 6, 5)
        item = item.copy(
            lastReplacedDate = serviceDate,
            lastReplacedOdometer = serviceOdometer,
            nextDueDate = newDueDate
        )

        // Reschedule calendar event
        scheduleRepo.addEvent(
            ScheduleEvent(
                id = item.scheduledEventId!!,
                title = "차량 정비 알림: Reduction Gear Oil",
                startTime = dateToInstant(newDueDate),
                endTime = dateToInstant(newDueDate),
                category = "CAR_MAINTENANCE",
                linkedCarRecordId = item.id
            )
        )

        // 3. Verify
        val (wear, status) = MaintenanceWearEngine.calculateWear(item, vehicle.currentOdometer, serviceDate)
        assertEquals(0.0, wear)
        assertEquals(MaintenanceStatus.OK, status)

        val updatedEvent = scheduleRepo.events.first { it.id == item.scheduledEventId }
        assertEquals(dateToInstant(newDueDate), updatedEvent.startTime)
        assertEquals("m_oil", updatedEvent.linkedCarRecordId)
    }

    @Test
    fun testSync_RefuelingUpdatesOdometer_TriggersMaintenanceWarning() {
        val vehicle = Vehicle("v1", "Sonata", "Sedan", FuelType.GASOLINE, 40000.0, 48000.0)
        val item = MaintenanceItem(
            id = "m_filter",
            vehicleId = "v1",
            partName = "Air Conditioner Filter",
            cycleKm = 10000.0,
            cycleMonths = 12,
            lastReplacedDate = LocalDate(2026, 1, 1),
            lastReplacedOdometer = 40000.0,
            nextDueDate = LocalDate(2027, 1, 1)
        )

        // Before refueling: 8,000 km driven -> 80% wear (WARNING)
        val (_, statusBefore) = MaintenanceWearEngine.calculateWear(item, vehicle.currentOdometer, LocalDate(2026, 5, 1))
        assertEquals(MaintenanceStatus.WARNING, statusBefore)

        // Refuel with high odometer entry: 50,200 km
        val refuelOdo = 50200.0
        vehicle.currentOdometer = refuelOdo

        // After refueling: 10,200 km driven -> 102% wear (OVERDUE)
        val (wearAfter, statusAfter) = MaintenanceWearEngine.calculateWear(item, vehicle.currentOdometer, LocalDate(2026, 5, 1))
        assertEquals(102.0, wearAfter)
        assertEquals(MaintenanceStatus.OVERDUE, statusAfter)
    }

    @Test
    fun testSync_UnifiedDailyTimeline_CombinesScheduleRoutineAndCarMaintenance() {
        val scheduleRepo = InMemoryScheduleRepository()
        val targetDate = LocalDate(2026, 11, 8)
        val targetInstant = dateToInstant(targetDate)

        // 1. Calendar Event on targetDate
        scheduleRepo.addEvent(
            ScheduleEvent("e1", title = "Quarterly Review", startTime = targetInstant, endTime = targetInstant)
        )

        // 2. Car Maintenance Event on targetDate
        scheduleRepo.addEvent(
            ScheduleEvent("e_maint", title = "Tire Rotation Due", startTime = targetInstant, endTime = targetInstant, category = "CAR_MAINTENANCE", linkedCarRecordId = "m_tire")
        )

        // 3. Routine for targetDate (Daily routine)
        val routine = Routine("r_stretch", "Morning Stretches", repeatDaysMask = 127, startDate = LocalDate(2026, 11, 1))
        scheduleRepo.addRoutine(routine)
        scheduleRepo.logRoutine(RoutineLog("l1", "r_stretch", targetDate, isCompleted = true))

        // Query daily events
        val dailyEvents = scheduleRepo.getEventsForRange(targetInstant, targetInstant)
        assertEquals(2, dailyEvents.size)

        val maintenanceEvent = dailyEvents.firstOrNull { it.category == "CAR_MAINTENANCE" }
        assertNotNull(maintenanceEvent)
        assertEquals("m_tire", maintenanceEvent.linkedCarRecordId)

        // Query routine streak
        val streak = RoutineStreakEngine.calculateStreak(routine, scheduleRepo.routineLogs, targetDate)
        assertEquals(1, streak)
    }
}
