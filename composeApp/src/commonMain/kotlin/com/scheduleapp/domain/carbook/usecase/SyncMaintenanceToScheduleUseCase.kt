package com.scheduleapp.domain.carbook.usecase

import com.scheduleapp.domain.calendar.model.ScheduleEvent
import com.scheduleapp.domain.calendar.repository.ScheduleRepository
import com.scheduleapp.domain.carbook.engine.MaintenanceWearEngine
import com.scheduleapp.domain.carbook.model.CarExpenseLog
import com.scheduleapp.domain.carbook.model.ExpenseType
import com.scheduleapp.domain.carbook.model.MaintenanceItem
import com.scheduleapp.domain.carbook.repository.CarBookRepository

class SyncMaintenanceToScheduleUseCase(
    private val carBookRepository: CarBookRepository,
    private val scheduleRepository: ScheduleRepository
) {

    /**
     * Creates a new MaintenanceItem and automatically registers a linked
     * ScheduleEvent in the calendar for the calculated nextDueDate.
     */
    suspend fun createMaintenanceItem(item: MaintenanceItem): Long {
        val nextDueDate = if (item.nextDueDate > 0L) {
            item.nextDueDate
        } else {
            MaintenanceWearEngine.calculateNextDueDate(item.lastReplacedDate, item.cycleMonths)
        }

        val initialItem = item.copy(nextDueDate = nextDueDate)
        val createdItemId = carBookRepository.insertMaintenanceItem(initialItem)

        // Create linked ScheduleEvent with vehicle name tag for multi-vehicle clarity
        val vehicle = carBookRepository.getVehicleById(item.vehicleId)
        val eventTitle = buildEventTitle(vehicle?.name, item.partName)
        val memo = buildEventMemo(vehicle?.name, vehicle?.model, item.cycleKm, item.cycleMonths)
        val scheduleEvent = ScheduleEvent(
            title = eventTitle,
            startTime = nextDueDate,
            endTime = nextDueDate + ONE_HOUR_MILLIS,
            isAllDay = true,
            category = CATEGORY_CAR_MAINTENANCE,
            linkedCarRecordId = createdItemId,
            memo = memo
        )
        val eventId = scheduleRepository.insertEvent(scheduleEvent)

        // Store back scheduledEventId
        carBookRepository.updateMaintenanceItem(
            initialItem.copy(id = createdItemId, scheduledEventId = eventId)
        )

        return createdItemId
    }

    /**
     * Updates an existing MaintenanceItem and syncs its linked ScheduleEvent in the calendar.
     */
    suspend fun updateMaintenanceItem(item: MaintenanceItem) {
        var eventId = item.scheduledEventId

        val existingEvent = if (eventId != null) scheduleRepository.getEventById(eventId) else null
        val vehicle = carBookRepository.getVehicleById(item.vehicleId)
        val eventTitle = buildEventTitle(vehicle?.name, item.partName)
        val memo = buildEventMemo(vehicle?.name, vehicle?.model, item.cycleKm, item.cycleMonths)

        if (existingEvent != null) {
            val updatedEvent = existingEvent.copy(
                title = eventTitle,
                startTime = item.nextDueDate,
                endTime = item.nextDueDate + ONE_HOUR_MILLIS,
                category = CATEGORY_CAR_MAINTENANCE,
                linkedCarRecordId = item.id,
                memo = memo
            )
            scheduleRepository.updateEvent(updatedEvent)
        } else {
            val newEvent = ScheduleEvent(
                title = eventTitle,
                startTime = item.nextDueDate,
                endTime = item.nextDueDate + ONE_HOUR_MILLIS,
                isAllDay = true,
                category = CATEGORY_CAR_MAINTENANCE,
                linkedCarRecordId = item.id,
                memo = memo
            )
            eventId = scheduleRepository.insertEvent(newEvent)
        }

        carBookRepository.updateMaintenanceItem(item.copy(scheduledEventId = eventId))
    }

    /**
     * Records maintenance service completion:
     * 1. Records maintenance cost in CarExpenseLog (type = MAINTENANCE).
     * 2. Updates vehicle current odometer if service mileage is higher.
     * 3. Resets wear baseline (lastReplacedDate, lastReplacedOdometer).
     * 4. Calculates new nextDueDate and reschedules the linked ScheduleEvent.
     */
    suspend fun logMaintenanceReplacement(
        itemId: Long,
        serviceDate: Long,
        serviceOdometer: Double,
        cost: Double,
        memo: String? = null
    ) {
        val item = carBookRepository.getMaintenanceItemById(itemId)
            ?: throw IllegalArgumentException("Maintenance item not found with id: $itemId")

        // 1. Add CarExpenseLog
        val expense = CarExpenseLog(
            vehicleId = item.vehicleId,
            date = serviceDate,
            type = ExpenseType.MAINTENANCE,
            amount = cost,
            odometer = serviceOdometer,
            memo = memo ?: "${item.partName} 교체"
        )
        carBookRepository.insertExpense(expense)

        // 2. Update vehicle odometer if higher
        val vehicle = carBookRepository.getVehicleById(item.vehicleId)
        if (vehicle != null && serviceOdometer > vehicle.currentOdometer) {
            carBookRepository.updateVehicleOdometer(item.vehicleId, serviceOdometer)
        }

        // 3. Calculate new next due date
        val newDueDate = MaintenanceWearEngine.calculateNextDueDate(serviceDate, item.cycleMonths)

        // 4. Reschedule or create linked calendar event
        var eventId = item.scheduledEventId
        val existingEvent = if (eventId != null) scheduleRepository.getEventById(eventId) else null
        val eventTitle = buildEventTitle(vehicle?.name, item.partName)
        val eventMemo = buildEventMemo(vehicle?.name, vehicle?.model, item.cycleKm, item.cycleMonths)

        if (existingEvent != null) {
            val updatedEvent = existingEvent.copy(
                title = eventTitle,
                startTime = newDueDate,
                endTime = newDueDate + ONE_HOUR_MILLIS,
                memo = eventMemo
            )
            scheduleRepository.updateEvent(updatedEvent)
        } else {
            val newEvent = ScheduleEvent(
                title = eventTitle,
                startTime = newDueDate,
                endTime = newDueDate + ONE_HOUR_MILLIS,
                isAllDay = true,
                category = CATEGORY_CAR_MAINTENANCE,
                linkedCarRecordId = item.id,
                memo = eventMemo
            )
            eventId = scheduleRepository.insertEvent(newEvent)
        }

        // 5. Update MaintenanceItem baseline
        val updatedItem = item.copy(
            lastReplacedDate = serviceDate,
            lastReplacedOdometer = serviceOdometer,
            nextDueDate = newDueDate,
            scheduledEventId = eventId
        )
        carBookRepository.updateMaintenanceItem(updatedItem)
    }

    /**
     * Deletes a maintenance item and removes its linked ScheduleEvent from the calendar.
     */
    suspend fun deleteMaintenanceItem(itemId: Long) {
        val item = carBookRepository.getMaintenanceItemById(itemId)
        if (item != null) {
            item.scheduledEventId?.let { eventId ->
                val event = scheduleRepository.getEventById(eventId)
                if (event != null) {
                    scheduleRepository.deleteEvent(event)
                }
            }
            carBookRepository.deleteMaintenanceItem(item)
        }
    }

    private fun buildEventTitle(vehicleName: String?, partName: String): String {
        return if (!vehicleName.isNullOrBlank()) {
            "[차량 정비 - $vehicleName] $partName 교체 주기 도래"
        } else {
            "[차량 정비] $partName 교체 주기 도래"
        }
    }

    private fun buildEventMemo(vehicleName: String?, vehicleModel: String?, cycleKm: Double, cycleMonths: Int): String {
        val vehiclePrefix = if (!vehicleName.isNullOrBlank()) {
            "대상 차량: $vehicleName${if (!vehicleModel.isNullOrBlank()) " ($vehicleModel)" else ""} | "
        } else {
            ""
        }
        return "${vehiclePrefix}소모품 교체 주기: ${cycleKm.toLong()} km / ${cycleMonths}개월"
    }

    companion object {
        const val CATEGORY_CAR_MAINTENANCE = "CAR_MAINTENANCE"
        private const val ONE_HOUR_MILLIS = 3600_000L
    }
}
