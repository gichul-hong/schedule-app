package com.scheduleapp.core.platform

import com.scheduleapp.core.database.entity.CarExpenseLogEntity
import com.scheduleapp.core.database.entity.FuelDetailEntity
import com.scheduleapp.core.database.entity.MaintenanceItemEntity
import com.scheduleapp.core.database.entity.RoutineEntity
import com.scheduleapp.core.database.entity.RoutineLogEntity
import com.scheduleapp.core.database.entity.ScheduleEventEntity
import com.scheduleapp.core.database.entity.TodoItemEntity
import com.scheduleapp.core.database.entity.VehicleEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DatabaseBackupEngineTest {

    @Test
    fun testMiniJsonParser_PrimitivesAndEscaping() {
        val json = """
            {
                "str": "Hello \"World\"\nNew Line",
                "int": 42,
                "float": 3.14159,
                "bool": true,
                "nullVal": null
            }
        """.trimIndent()

        val parser = MiniJsonParser(json)
        @Suppress("UNCHECKED_CAST")
        val result = parser.parse() as Map<String, Any?>

        assertEquals("Hello \"World\"\nNew Line", result["str"])
        assertEquals(42L, (result["int"] as Number).toLong())
        assertEquals(3.14159, (result["float"] as Number).toDouble(), 0.0001)
        assertEquals(true, result["bool"])
        assertEquals(null, result["nullVal"])
    }

    @Test
    fun testMiniJsonParser_ArraysAndNestedObjects() {
        val json = """
            {
                "items": [
                    {"id": 1, "name": "Item 1"},
                    {"id": 2, "name": "Item 2"}
                ]
            }
        """.trimIndent()

        val parser = MiniJsonParser(json)
        @Suppress("UNCHECKED_CAST")
        val result = parser.parse() as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val items = result["items"] as List<Map<String, Any?>>

        assertEquals(2, items.size)
        assertEquals(1L, (items[0]["id"] as Number).toLong())
        assertEquals("Item 1", items[0]["name"])
        assertEquals(2L, (items[1]["id"] as Number).toLong())
        assertEquals("Item 2", items[1]["name"])
    }

    @Test
    fun testSnapshot_SerializationAndDeserialization_Roundtrip() {
        val event = ScheduleEventEntity(
            id = 101L,
            externalEventId = "ext_101",
            title = "정기 엔진오일 교환",
            startTime = 1731050000000L,
            endTime = 1731053600000L,
            isAllDay = false,
            category = "MAINTENANCE",
            linkedCarRecordId = 501L
        )

        val todo = TodoItemEntity(
            id = 201L,
            title = "와이퍼 블레이드 구매",
            dueDate = 1731100000000L,
            priority = 3,
            isCompleted = true,
            completedAt = 1731090000000L
        )

        val routine = RoutineEntity(
            id = 301L,
            title = "차량 주유 및 주행거리 기록",
            repeatDaysMask = 127,
            targetTime = "09:00",
            category = "DAILY",
            createdAt = 1730000000000L
        )

        val routineLog = RoutineLogEntity(
            id = 401L,
            routineId = 301L,
            date = "2026-11-08",
            isCompleted = true
        )

        val vehicle = VehicleEntity(
            id = 1L,
            name = "아반떼 CN7",
            model = "1.6 가솔린 인스퍼레이션",
            fuelType = "GASOLINE",
            initialOdometer = 15000.0,
            currentOdometer = 24500.0,
            isDefault = true
        )

        val expense = CarExpenseLogEntity(
            id = 501L,
            vehicleId = 1L,
            date = 1731050000000L,
            type = "MAINTENANCE",
            amount = 85000.0,
            odometer = 24500.0,
            memo = "엔진오일 및 에어컨필터 교체"
        )

        val fuelDetail = FuelDetailEntity(
            expenseId = 601L,
            unitPrice = 1680.0,
            liters = 42.5,
            isFullTank = true,
            calculatedEfficiency = 14.8
        )

        val maintenanceItem = MaintenanceItemEntity(
            id = 701L,
            vehicleId = 1L,
            partName = "엔진오일",
            cycleKm = 10000.0,
            cycleMonths = 12,
            lastReplacedDate = 1731050000000L,
            lastReplacedOdometer = 24500.0,
            nextDueDate = 1762586000000L,
            scheduledEventId = 101L
        )

        val originalSnapshot = DatabaseSnapshot(
            version = 1,
            exportedAt = 1731080000000L,
            events = listOf(event),
            todos = listOf(todo),
            routines = listOf(routine),
            routineLogs = listOf(routineLog),
            vehicles = listOf(vehicle),
            expenses = listOf(expense),
            fuelDetails = listOf(fuelDetail),
            maintenanceItems = listOf(maintenanceItem)
        )

        assertEquals(8, originalSnapshot.totalRecords)

        // Serialize
        val json = DatabaseBackupEngine.serializeSnapshot(originalSnapshot)
        assertNotNull(json)
        assertTrue(json.contains("\"아반떼 CN7\""))
        assertTrue(json.contains("\"엔진오일 및 에어컨필터 교체\""))

        // Deserialize
        val parsedSnapshot = DatabaseBackupEngine.parseSnapshot(json)

        assertEquals(originalSnapshot.version, parsedSnapshot.version)
        assertEquals(originalSnapshot.exportedAt, parsedSnapshot.exportedAt)
        assertEquals(8, parsedSnapshot.totalRecords)

        // Verify Event
        assertEquals(1, parsedSnapshot.events.size)
        val pEvent = parsedSnapshot.events[0]
        assertEquals(event.id, pEvent.id)
        assertEquals(event.externalEventId, pEvent.externalEventId)
        assertEquals(event.title, pEvent.title)
        assertEquals(event.startTime, pEvent.startTime)
        assertEquals(event.endTime, pEvent.endTime)
        assertEquals(event.isAllDay, pEvent.isAllDay)
        assertEquals(event.category, pEvent.category)
        assertEquals(event.linkedCarRecordId, pEvent.linkedCarRecordId)

        // Verify Todo
        assertEquals(1, parsedSnapshot.todos.size)
        val pTodo = parsedSnapshot.todos[0]
        assertEquals(todo.id, pTodo.id)
        assertEquals(todo.title, pTodo.title)
        assertEquals(todo.priority, pTodo.priority)
        assertEquals(todo.isCompleted, pTodo.isCompleted)
        assertEquals(todo.completedAt, pTodo.completedAt)

        // Verify Vehicle
        assertEquals(1, parsedSnapshot.vehicles.size)
        val pVehicle = parsedSnapshot.vehicles[0]
        assertEquals(vehicle.id, pVehicle.id)
        assertEquals(vehicle.name, pVehicle.name)
        assertEquals(vehicle.model, pVehicle.model)
        assertEquals(vehicle.fuelType, pVehicle.fuelType)
        assertEquals(vehicle.initialOdometer, pVehicle.initialOdometer)
        assertEquals(vehicle.currentOdometer, pVehicle.currentOdometer)
        assertEquals(vehicle.isDefault, pVehicle.isDefault)

        // Verify Expense
        assertEquals(1, parsedSnapshot.expenses.size)
        val pExpense = parsedSnapshot.expenses[0]
        assertEquals(expense.id, pExpense.id)
        assertEquals(expense.vehicleId, pExpense.vehicleId)
        assertEquals(expense.amount, pExpense.amount)
        assertEquals(expense.odometer, pExpense.odometer)
        assertEquals(expense.memo, pExpense.memo)

        // Verify FuelDetail
        assertEquals(1, parsedSnapshot.fuelDetails.size)
        val pFuel = parsedSnapshot.fuelDetails[0]
        assertEquals(fuelDetail.expenseId, pFuel.expenseId)
        assertEquals(fuelDetail.unitPrice, pFuel.unitPrice)
        assertEquals(fuelDetail.liters, pFuel.liters)
        assertEquals(fuelDetail.isFullTank, pFuel.isFullTank)
        assertEquals(fuelDetail.calculatedEfficiency, pFuel.calculatedEfficiency)

        // Verify MaintenanceItem
        assertEquals(1, parsedSnapshot.maintenanceItems.size)
        val pMaint = parsedSnapshot.maintenanceItems[0]
        assertEquals(maintenanceItem.id, pMaint.id)
        assertEquals(maintenanceItem.partName, pMaint.partName)
        assertEquals(maintenanceItem.cycleKm, pMaint.cycleKm)
        assertEquals(maintenanceItem.cycleMonths, pMaint.cycleMonths)
        assertEquals(maintenanceItem.lastReplacedOdometer, pMaint.lastReplacedOdometer)
        assertEquals(maintenanceItem.scheduledEventId, pMaint.scheduledEventId)
    }

    @Test
    fun testSnapshot_EmptyLists() {
        val emptySnapshot = DatabaseSnapshot(
            version = 1,
            exportedAt = 123456789L
        )
        val json = DatabaseBackupEngine.serializeSnapshot(emptySnapshot)
        val parsed = DatabaseBackupEngine.parseSnapshot(json)

        assertEquals(1, parsed.version)
        assertEquals(123456789L, parsed.exportedAt)
        assertEquals(0, parsed.totalRecords)
        assertTrue(parsed.events.isEmpty())
        assertTrue(parsed.vehicles.isEmpty())
    }

    @Test
    fun testSnapshot_CorruptJson_ThrowsException() {
        assertFailsWith<IllegalArgumentException> {
            DatabaseBackupEngine.parseSnapshot("{ invalid_json: 123")
        }

        assertFailsWith<IllegalArgumentException> {
            DatabaseBackupEngine.parseSnapshot("not a json string")
        }
    }
}
