package com.scheduleapp.domain.carbook

import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.calendar.model.Routine
import com.scheduleapp.domain.calendar.model.RoutineLog
import com.scheduleapp.domain.calendar.model.ScheduleEvent
import com.scheduleapp.domain.calendar.model.TodoItem
import com.scheduleapp.domain.calendar.repository.ScheduleRepository
import com.scheduleapp.domain.carbook.model.CarExpenseLog
import com.scheduleapp.domain.carbook.model.ExpenseType
import com.scheduleapp.domain.carbook.model.FuelDetail
import com.scheduleapp.domain.carbook.model.FuelRecord
import com.scheduleapp.domain.carbook.model.FuelType
import com.scheduleapp.domain.carbook.model.MaintenanceItem
import com.scheduleapp.domain.carbook.model.Vehicle
import com.scheduleapp.domain.carbook.repository.CarBookRepository
import com.scheduleapp.domain.carbook.usecase.SyncMaintenanceToScheduleUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeScheduleRepository : ScheduleRepository {
    val events = mutableListOf<ScheduleEvent>()
    private var nextId = 1L

    override fun getAllEvents(): Flow<List<ScheduleEvent>> = flowOf(events)
    override fun getEventsInRange(start: Long, end: Long): Flow<List<ScheduleEvent>> =
        flowOf(events.filter { it.startTime in start..end })
    override suspend fun getEventById(id: Long): ScheduleEvent? = events.find { it.id == id }
    override suspend fun insertEvent(event: ScheduleEvent): Long {
        val id = if (event.id > 0) event.id else nextId++
        events.removeAll { it.id == id }
        events.add(event.copy(id = id))
        return id
    }
    override suspend fun updateEvent(event: ScheduleEvent) {
        val index = events.indexOfFirst { it.id == event.id }
        if (index != -1) events[index] = event
    }
    override suspend fun deleteEvent(event: ScheduleEvent) {
        events.removeAll { it.id == event.id }
    }

    override fun getAllTodos(): Flow<List<TodoItem>> = flowOf(emptyList())
    override fun getTodosByDate(start: Long, end: Long): Flow<List<TodoItem>> = flowOf(emptyList())
    override suspend fun getTodoById(id: Long): TodoItem? = null
    override suspend fun insertTodo(todo: TodoItem): Long = 1L
    override suspend fun updateTodo(todo: TodoItem) {}
    override suspend fun deleteTodo(todo: TodoItem) {}
    override suspend fun toggleTodoCompletion(id: Long, isCompleted: Boolean) {}
    override fun getAllRoutines(): Flow<List<Routine>> = flowOf(emptyList())
    override suspend fun getRoutineById(id: Long): Routine? = null
    override suspend fun insertRoutine(routine: Routine): Long = 1L
    override suspend fun updateRoutine(routine: Routine) {}
    override suspend fun deleteRoutine(routine: Routine) {}
    override fun getAllRoutineLogs(): Flow<List<RoutineLog>> = flowOf(emptyList())
    override fun getRoutineLogsForDate(date: String): Flow<List<RoutineLog>> = flowOf(emptyList())
    override fun getRoutineLogsForRoutine(routineId: Long): Flow<List<RoutineLog>> = flowOf(emptyList())
    override suspend fun insertRoutineLog(log: RoutineLog): Long = 1L
    override suspend fun updateRoutineLog(log: RoutineLog) {}
    override suspend fun deleteRoutineLog(log: RoutineLog) {}
    override suspend fun toggleRoutineCompletion(routineId: Long, date: String, isCompleted: Boolean) {}
}

class FakeCarBookRepository : CarBookRepository {
    val vehicles = mutableListOf<Vehicle>()
    val expenses = mutableListOf<CarExpenseLog>()
    val maintenanceItems = mutableListOf<MaintenanceItem>()
    private var nextExpenseId = 1L
    private var nextMaintId = 1L

    override fun getAllVehicles(): Flow<List<Vehicle>> = flowOf(vehicles)
    override fun getDefaultVehicle(): Flow<Vehicle?> = flowOf(vehicles.firstOrNull())
    override suspend fun getVehicleById(id: Long): Vehicle? = vehicles.find { it.id == id }
    override suspend fun insertVehicle(vehicle: Vehicle): Long = 1L
    override suspend fun updateVehicle(vehicle: Vehicle) {
        val index = vehicles.indexOfFirst { it.id == vehicle.id }
        if (index != -1) vehicles[index] = vehicle
    }
    override suspend fun deleteVehicle(vehicle: Vehicle) {}
    override suspend fun updateVehicleOdometer(vehicleId: Long, newOdometer: Double) {
        val index = vehicles.indexOfFirst { it.id == vehicleId }
        if (index != -1) {
            vehicles[index] = vehicles[index].copy(currentOdometer = newOdometer)
        }
    }

    override fun getAllExpenses(): Flow<List<CarExpenseLog>> = flowOf(expenses)
    override fun getExpensesForVehicle(vehicleId: Long): Flow<List<CarExpenseLog>> = flowOf(expenses.filter { it.vehicleId == vehicleId })
    override fun getExpensesForVehicleInRange(vehicleId: Long, start: Long, end: Long): Flow<List<CarExpenseLog>> = flowOf(expenses)
    override fun getExpensesByType(vehicleId: Long, type: ExpenseType): Flow<List<CarExpenseLog>> = flowOf(expenses.filter { it.type == type })
    override suspend fun insertExpense(expense: CarExpenseLog): Long {
        val id = if (expense.id > 0) expense.id else nextExpenseId++
        expenses.add(expense.copy(id = id))
        return id
    }
    override suspend fun updateExpense(expense: CarExpenseLog) {}
    override suspend fun deleteExpense(expense: CarExpenseLog) {}
    override suspend fun deleteExpenseById(id: Long) {}

    override fun getFuelRecordsForVehicle(vehicleId: Long): Flow<List<FuelRecord>> = flowOf(emptyList())
    override suspend fun getFuelDetail(expenseId: Long): FuelDetail? = null
    override suspend fun insertFuelRecord(fuelRecord: FuelRecord): Long = 1L
    override suspend fun updateFuelRecord(fuelRecord: FuelRecord) {}
    override suspend fun deleteFuelRecord(expenseId: Long) {}

    override fun getMaintenanceItemsForVehicle(vehicleId: Long): Flow<List<MaintenanceItem>> = flowOf(maintenanceItems)
    override suspend fun getMaintenanceItemById(id: Long): MaintenanceItem? = maintenanceItems.find { it.id == id }
    override suspend fun insertMaintenanceItem(item: MaintenanceItem): Long {
        val id = if (item.id > 0) item.id else nextMaintId++
        maintenanceItems.add(item.copy(id = id))
        return id
    }
    override suspend fun updateMaintenanceItem(item: MaintenanceItem) {
        val index = maintenanceItems.indexOfFirst { it.id == item.id }
        if (index != -1) maintenanceItems[index] = item
    }
    override suspend fun deleteMaintenanceItem(item: MaintenanceItem) {
        maintenanceItems.removeAll { it.id == item.id }
    }
    override suspend fun deleteMaintenanceItemById(id: Long) {
        maintenanceItems.removeAll { it.id == id }
    }
}

class SyncMaintenanceToScheduleUseCaseTest {

    @Test
    fun testCreateMaintenanceItem_AutoCreatesScheduleEvent() = runTest {
        val scheduleRepo = FakeScheduleRepository()
        val carBookRepo = FakeCarBookRepository()
        val useCase = SyncMaintenanceToScheduleUseCase(carBookRepo, scheduleRepo)

        val vehicle = Vehicle(id = 1L, name = "Sonata", model = "DN8", fuelType = FuelType.GASOLINE, initialOdometer = 40000.0, currentOdometer = 45000.0)
        carBookRepo.vehicles.add(vehicle)

        val lastDateMillis = DateTimeUtils.startOfDayMillis(LocalDate(2026, 1, 1))
        val item = MaintenanceItem(
            vehicleId = 1L,
            partName = "엔진오일 & 필터",
            cycleKm = 10000.0,
            cycleMonths = 12,
            lastReplacedDate = lastDateMillis,
            lastReplacedOdometer = 40000.0,
            nextDueDate = 0L // Should be calculated to 2027-01-01
        )

        val createdId = useCase.createMaintenanceItem(item)
        val savedItem = carBookRepo.getMaintenanceItemById(createdId)
        assertNotNull(savedItem)
        assertNotNull(savedItem.scheduledEventId)

        // Verify linked calendar event
        val event = scheduleRepo.getEventById(savedItem.scheduledEventId!!)
        assertNotNull(event)
        assertEquals(SyncMaintenanceToScheduleUseCase.CATEGORY_CAR_MAINTENANCE, event.category)
        assertEquals("[차량 정비 - Sonata] 엔진오일 & 필터 교체 주기 도래", event.title)
        assertEquals(createdId, event.linkedCarRecordId)
        val expectedDueDate = DateTimeUtils.startOfDayMillis(LocalDate(2027, 1, 1))
        assertEquals(expectedDueDate, event.startTime)
    }

    @Test
    fun testMultiVehicle_TwoCarsCreateDistinctCalendarEventsAndIndependentOdometers() = runTest {
        val scheduleRepo = FakeScheduleRepository()
        val carBookRepo = FakeCarBookRepository()
        val useCase = SyncMaintenanceToScheduleUseCase(carBookRepo, scheduleRepo)

        val car1 = Vehicle(id = 1L, name = "1호차 (메인)", model = "아반떼 CN7", fuelType = FuelType.GASOLINE, initialOdometer = 10000.0, currentOdometer = 15000.0, isDefault = true)
        val car2 = Vehicle(id = 2L, name = "2호차 (세컨)", model = "쏘렌토 MQ4", fuelType = FuelType.HYBRID, initialOdometer = 25000.0, currentOdometer = 32000.0, isDefault = false)
        carBookRepo.vehicles.add(car1)
        carBookRepo.vehicles.add(car2)

        val itemCar1Id = useCase.createMaintenanceItem(
            MaintenanceItem(
                vehicleId = 1L,
                partName = "엔진오일",
                cycleKm = 10000.0,
                cycleMonths = 12,
                lastReplacedDate = DateTimeUtils.startOfDayMillis(LocalDate(2026, 1, 1)),
                lastReplacedOdometer = 10000.0,
                nextDueDate = 0L
            )
        )
        val itemCar2Id = useCase.createMaintenanceItem(
            MaintenanceItem(
                vehicleId = 2L,
                partName = "에어컨 필터",
                cycleKm = 15000.0,
                cycleMonths = 6,
                lastReplacedDate = DateTimeUtils.startOfDayMillis(LocalDate(2026, 2, 1)),
                lastReplacedOdometer = 25000.0,
                nextDueDate = 0L
            )
        )

        val savedCar1Item = carBookRepo.getMaintenanceItemById(itemCar1Id)!!
        val savedCar2Item = carBookRepo.getMaintenanceItemById(itemCar2Id)!!

        val eventCar1 = scheduleRepo.getEventById(savedCar1Item.scheduledEventId!!)!!
        val eventCar2 = scheduleRepo.getEventById(savedCar2Item.scheduledEventId!!)!!

        assertEquals("[차량 정비 - 1호차 (메인)] 엔진오일 교체 주기 도래", eventCar1.title)
        assertEquals("[차량 정비 - 2호차 (세컨)] 에어컨 필터 교체 주기 도래", eventCar2.title)
        assertTrue(eventCar1.memo!!.contains("1호차 (메인)"))
        assertTrue(eventCar2.memo!!.contains("2호차 (세컨)"))
    }

    @Test
    fun testLogMaintenanceReplacement_RecordsExpense_ResetsWear_ReschedulesEvent() = runTest {
        val scheduleRepo = FakeScheduleRepository()
        val carBookRepo = FakeCarBookRepository()
        val useCase = SyncMaintenanceToScheduleUseCase(carBookRepo, scheduleRepo)

        val vehicle = Vehicle(id = 1L, name = "Ioniq 5", model = "EV", fuelType = FuelType.ELECTRIC, initialOdometer = 30000.0, currentOdometer = 40000.0)
        carBookRepo.vehicles.add(vehicle)

        val lastDateMillis = DateTimeUtils.startOfDayMillis(LocalDate(2025, 6, 1))
        val initialItem = MaintenanceItem(
            vehicleId = 1L,
            partName = "감속기 오일",
            cycleKm = 20000.0,
            cycleMonths = 12,
            lastReplacedDate = lastDateMillis,
            lastReplacedOdometer = 30000.0,
            nextDueDate = 0L
        )
        val itemId = useCase.createMaintenanceItem(initialItem)

        // User performs service at 50,500 km on 2026-06-05 costing 150,000 KRW
        val serviceDate = DateTimeUtils.startOfDayMillis(LocalDate(2026, 6, 5))
        val serviceOdometer = 50500.0
        val serviceCost = 150000.0

        useCase.logMaintenanceReplacement(
            itemId = itemId,
            serviceDate = serviceDate,
            serviceOdometer = serviceOdometer,
            cost = serviceCost,
            memo = "공식 서비스센터 감속기오일 교환"
        )

        // 1. Verify CarExpenseLog recorded
        assertEquals(1, carBookRepo.expenses.size)
        val expense = carBookRepo.expenses.first()
        assertEquals(ExpenseType.MAINTENANCE, expense.type)
        assertEquals(serviceCost, expense.amount)
        assertEquals(serviceOdometer, expense.odometer)

        // 2. Verify Vehicle odometer updated
        assertEquals(serviceOdometer, carBookRepo.getVehicleById(1L)?.currentOdometer)

        // 3. Verify MaintenanceItem reset
        val updatedItem = carBookRepo.getMaintenanceItemById(itemId)
        assertNotNull(updatedItem)
        assertEquals(serviceDate, updatedItem.lastReplacedDate)
        assertEquals(serviceOdometer, updatedItem.lastReplacedOdometer)

        val expectedNextDue = DateTimeUtils.startOfDayMillis(LocalDate(2027, 6, 5))
        assertEquals(expectedNextDue, updatedItem.nextDueDate)

        // 4. Verify ScheduleEvent rescheduled
        val event = scheduleRepo.getEventById(updatedItem.scheduledEventId!!)
        assertNotNull(event)
        assertEquals(expectedNextDue, event.startTime)
    }

    @Test
    fun testDeleteMaintenanceItem_RemovesLinkedScheduleEvent() = runTest {
        val scheduleRepo = FakeScheduleRepository()
        val carBookRepo = FakeCarBookRepository()
        val useCase = SyncMaintenanceToScheduleUseCase(carBookRepo, scheduleRepo)

        val item = MaintenanceItem(
            vehicleId = 1L,
            partName = "에어컨 필터",
            cycleKm = 10000.0,
            cycleMonths = 6,
            lastReplacedDate = DateTimeUtils.nowMillis(),
            lastReplacedOdometer = 10000.0,
            nextDueDate = 0L
        )
        val itemId = useCase.createMaintenanceItem(item)
        val created = carBookRepo.getMaintenanceItemById(itemId)
        assertNotNull(created)
        val eventId = created.scheduledEventId
        assertNotNull(eventId)
        assertNotNull(scheduleRepo.getEventById(eventId))

        // Delete maintenance item
        useCase.deleteMaintenanceItem(itemId)

        // Verify both item and event removed
        assertNull(carBookRepo.getMaintenanceItemById(itemId))
        assertNull(scheduleRepo.getEventById(eventId))
    }
}
