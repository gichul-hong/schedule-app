package com.scheduleapp.features.settings

import com.scheduleapp.core.database.dao.CarBookDao
import com.scheduleapp.core.database.dao.ScheduleDao
import com.scheduleapp.core.database.entity.CarExpenseLogEntity
import com.scheduleapp.core.database.entity.FuelDetailEntity
import com.scheduleapp.core.database.entity.MaintenanceItemEntity
import com.scheduleapp.core.database.entity.RoutineEntity
import com.scheduleapp.core.database.entity.RoutineLogEntity
import com.scheduleapp.core.database.entity.ScheduleEventEntity
import com.scheduleapp.core.database.entity.TodoItemEntity
import com.scheduleapp.core.database.entity.VehicleEntity
import com.scheduleapp.core.platform.DatabaseBackupEngine
import com.scheduleapp.core.platform.DatabaseSnapshot
import com.scheduleapp.core.theme.ThemeMode
import com.scheduleapp.domain.carbook.model.CarExpenseLog
import com.scheduleapp.domain.carbook.model.ExpenseType
import com.scheduleapp.domain.carbook.model.FuelDetail
import com.scheduleapp.domain.carbook.model.FuelRecord
import com.scheduleapp.domain.carbook.model.FuelType
import com.scheduleapp.domain.carbook.model.MaintenanceItem
import com.scheduleapp.domain.carbook.model.Vehicle
import com.scheduleapp.domain.carbook.repository.CarBookRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private class FakeCarBookRepository : CarBookRepository {
        val vehiclesList = MutableStateFlow<List<Vehicle>>(
            listOf(
                Vehicle(1L, "아반떼", "CN7", FuelType.GASOLINE, 10000.0, 15000.0, isDefault = true),
                Vehicle(2L, "쏘렌토", "MQ4", FuelType.HYBRID, 5000.0, 8000.0, isDefault = false)
            )
        )

        override fun getAllVehicles(): Flow<List<Vehicle>> = vehiclesList
        override fun getDefaultVehicle(): Flow<Vehicle?> = flowOf(vehiclesList.value.find { it.isDefault })
        override suspend fun getVehicleById(id: Long): Vehicle? = vehiclesList.value.find { it.id == id }
        override suspend fun insertVehicle(vehicle: Vehicle): Long = 3L
        override suspend fun updateVehicle(vehicle: Vehicle) {
            vehiclesList.value = vehiclesList.value.map {
                if (it.id == vehicle.id) vehicle else it
            }
        }
        override suspend fun deleteVehicle(vehicle: Vehicle) {}
        override suspend fun updateVehicleOdometer(vehicleId: Long, newOdometer: Double) {}

        override fun getAllExpenses(): Flow<List<CarExpenseLog>> = flowOf(emptyList())
        override fun getExpensesForVehicle(vehicleId: Long): Flow<List<CarExpenseLog>> = flowOf(emptyList())
        override fun getExpensesForVehicleInRange(vehicleId: Long, start: Long, end: Long): Flow<List<CarExpenseLog>> = flowOf(emptyList())
        override fun getExpensesByType(vehicleId: Long, type: ExpenseType): Flow<List<CarExpenseLog>> = flowOf(emptyList())
        override suspend fun insertExpense(expense: CarExpenseLog): Long = 1L
        override suspend fun updateExpense(expense: CarExpenseLog) {}
        override suspend fun deleteExpense(expense: CarExpenseLog) {}
        override suspend fun deleteExpenseById(id: Long) {}

        override fun getFuelRecordsForVehicle(vehicleId: Long): Flow<List<FuelRecord>> = flowOf(emptyList())
        override suspend fun getFuelDetail(expenseId: Long): FuelDetail? = null
        override suspend fun insertFuelRecord(fuelRecord: FuelRecord): Long = 1L
        override suspend fun updateFuelRecord(fuelRecord: FuelRecord) {}
        override suspend fun deleteFuelRecord(expenseId: Long) {}

        override fun getMaintenanceItemsForVehicle(vehicleId: Long): Flow<List<MaintenanceItem>> = flowOf(emptyList())
        override suspend fun getMaintenanceItemById(id: Long): MaintenanceItem? = null
        override suspend fun insertMaintenanceItem(item: MaintenanceItem): Long = 1L
        override suspend fun updateMaintenanceItem(item: MaintenanceItem) {}
        override suspend fun deleteMaintenanceItem(item: MaintenanceItem) {}
        override suspend fun deleteMaintenanceItemById(id: Long) {}
    }

    private class FakeScheduleDao : ScheduleDao {
        override fun getAllEvents(): Flow<List<ScheduleEventEntity>> = flowOf(emptyList())
        override fun getEventsInRange(start: Long, end: Long): Flow<List<ScheduleEventEntity>> = flowOf(emptyList())
        override suspend fun getEventById(id: Long): ScheduleEventEntity? = null
        override suspend fun insertEvent(event: ScheduleEventEntity): Long = 1L
        override suspend fun updateEvent(event: ScheduleEventEntity) {}
        override suspend fun deleteEvent(event: ScheduleEventEntity) {}

        override fun getAllTodos(): Flow<List<TodoItemEntity>> = flowOf(emptyList())
        override fun getTodosByDate(start: Long, end: Long): Flow<List<TodoItemEntity>> = flowOf(emptyList())
        override suspend fun getTodoById(id: Long): TodoItemEntity? = null
        override suspend fun insertTodo(todo: TodoItemEntity): Long = 1L
        override suspend fun updateTodo(todo: TodoItemEntity) {}
        override suspend fun deleteTodo(todo: TodoItemEntity) {}

        override fun getAllRoutines(): Flow<List<RoutineEntity>> = flowOf(emptyList())
        override suspend fun getRoutineById(id: Long): RoutineEntity? = null
        override suspend fun insertRoutine(routine: RoutineEntity): Long = 1L
        override suspend fun updateRoutine(routine: RoutineEntity) {}
        override suspend fun deleteRoutine(routine: RoutineEntity) {}

        override fun getAllRoutineLogs(): Flow<List<RoutineLogEntity>> = flowOf(emptyList())
        override fun getRoutineLogsForDate(date: String): Flow<List<RoutineLogEntity>> = flowOf(emptyList())
        override fun getRoutineLogsForRoutine(routineId: Long): Flow<List<RoutineLogEntity>> = flowOf(emptyList())
        override suspend fun getRoutineLog(routineId: Long, date: String): RoutineLogEntity? = null
        override suspend fun insertRoutineLog(log: RoutineLogEntity): Long = 1L
        override suspend fun updateRoutineLog(log: RoutineLogEntity) {}
        override suspend fun deleteRoutineLog(log: RoutineLogEntity) {}

        override suspend fun insertEvents(events: List<ScheduleEventEntity>) {}
        override suspend fun insertTodos(todos: List<TodoItemEntity>) {}
        override suspend fun insertRoutines(routines: List<RoutineEntity>) {}
        override suspend fun insertRoutineLogs(logs: List<RoutineLogEntity>) {}
        override suspend fun clearAllEvents() {}
        override suspend fun clearAllTodos() {}
        override suspend fun clearAllRoutineLogs() {}
        override suspend fun clearAllRoutines() {}
    }

    private class FakeCarBookDao : CarBookDao {
        override fun getAllVehicles(): Flow<List<VehicleEntity>> = flowOf(listOf(
            VehicleEntity(1L, "아반떼", "CN7", "GASOLINE", 10000.0, 15000.0, true)
        ))
        override fun getDefaultVehicle(): Flow<VehicleEntity?> = flowOf(
            VehicleEntity(1L, "아반떼", "CN7", "GASOLINE", 10000.0, 15000.0, true)
        )
        override suspend fun getVehicleById(id: Long): VehicleEntity? = null
        override suspend fun insertVehicle(vehicle: VehicleEntity): Long = 1L
        override suspend fun updateVehicle(vehicle: VehicleEntity) {}
        override suspend fun deleteVehicle(vehicle: VehicleEntity) {}

        override fun getExpensesForVehicle(vehicleId: Long): Flow<List<CarExpenseLogEntity>> = flowOf(emptyList())
        override fun getExpensesForVehicleInRange(vehicleId: Long, start: Long, end: Long): Flow<List<CarExpenseLogEntity>> = flowOf(emptyList())
        override fun getExpensesByType(vehicleId: Long, type: String): Flow<List<CarExpenseLogEntity>> = flowOf(emptyList())
        override suspend fun insertExpense(expense: CarExpenseLogEntity): Long = 1L
        override suspend fun updateExpense(expense: CarExpenseLogEntity) {}
        override suspend fun getExpenseById(id: Long): CarExpenseLogEntity? = null
        override suspend fun deleteExpenseById(id: Long) {}
        override suspend fun deleteExpense(expense: CarExpenseLogEntity) {}

        override fun getAllFuelDetails(): Flow<List<FuelDetailEntity>> = flowOf(emptyList())
        override suspend fun getFuelDetail(expenseId: Long): FuelDetailEntity? = null
        override suspend fun insertFuelDetail(fuelDetail: FuelDetailEntity) {}
        override suspend fun updateFuelDetail(fuelDetail: FuelDetailEntity) {}
        override suspend fun deleteFuelDetail(fuelDetail: FuelDetailEntity) {}

        override fun getMaintenanceItemsForVehicle(vehicleId: Long): Flow<List<MaintenanceItemEntity>> = flowOf(emptyList())
        override suspend fun getMaintenanceItemById(id: Long): MaintenanceItemEntity? = null
        override suspend fun insertMaintenanceItem(item: MaintenanceItemEntity): Long = 1L
        override suspend fun updateMaintenanceItem(item: MaintenanceItemEntity) {}
        override suspend fun deleteMaintenanceItem(item: MaintenanceItemEntity) {}

        override fun getAllExpenses(): Flow<List<CarExpenseLogEntity>> = flowOf(emptyList())
        override fun getAllMaintenanceItems(): Flow<List<MaintenanceItemEntity>> = flowOf(emptyList())
        override suspend fun insertVehicles(vehicles: List<VehicleEntity>) {}
        override suspend fun insertExpenses(expenses: List<CarExpenseLogEntity>) {}
        override suspend fun insertFuelDetails(details: List<FuelDetailEntity>) {}
        override suspend fun insertMaintenanceItems(items: List<MaintenanceItemEntity>) {}
        override suspend fun clearAllFuelDetails() {}
        override suspend fun clearAllExpenses() {}
        override suspend fun clearAllMaintenanceItems() {}
        override suspend fun clearAllVehicles() {}
    }

    @Test
    fun testSettingsState_Defaults() {
        val state = SettingsState()
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertEquals(DistanceUnit.KM, state.distanceUnit)
        assertEquals(VolumeUnit.LITER, state.volumeUnit)
        assertEquals("1.0.0-MVP", state.appVersion)
        assertFalse(state.isExporting)
        assertFalse(state.isRestoring)
        assertNull(state.exportedJson)
        assertNull(state.statusMessage)
    }

    @Test
    fun testPreferencesMutation_ThemeAndUnits() = runTest {
        val repo = FakeCarBookRepository()
        val backupEngine = DatabaseBackupEngine(FakeScheduleDao(), FakeCarBookDao())
        val vm = SettingsViewModel(repo, backupEngine, gitHubSyncEngine = null, scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        // Theme Mode
        vm.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, vm.uiState.value.themeMode)

        vm.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, vm.uiState.value.themeMode)

        // Distance Unit
        vm.setDistanceUnit(DistanceUnit.MILE)
        assertEquals(DistanceUnit.MILE, vm.uiState.value.distanceUnit)

        // Volume Unit
        vm.setVolumeUnit(VolumeUnit.GALLON)
        assertEquals(VolumeUnit.GALLON, vm.uiState.value.volumeUnit)
    }

    @Test
    fun testDefaultVehicleSelection() = runTest {
        val repo = FakeCarBookRepository()
        val backupEngine = DatabaseBackupEngine(FakeScheduleDao(), FakeCarBookDao())
        val vm = SettingsViewModel(repo, backupEngine, gitHubSyncEngine = null, scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        // Change default vehicle to Sorento (id 2)
        vm.setDefaultVehicle(2L)

        val updatedDefault = vm.uiState.value.defaultVehicle
        assertNotNull(updatedDefault)
        assertEquals(2L, updatedDefault.id)
    }

    @Test
    fun testExportAndRestoreLifecycle() = runTest {
        val repo = FakeCarBookRepository()
        val backupEngine = DatabaseBackupEngine(FakeScheduleDao(), FakeCarBookDao())
        val vm = SettingsViewModel(repo, backupEngine, gitHubSyncEngine = null, scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        // Export
        vm.exportBackup()

        val stateAfterExport = vm.uiState.value
        assertNotNull(stateAfterExport.exportedJson)
        assertNotNull(stateAfterExport.statusMessage)
        assertFalse(stateAfterExport.isStatusError)
        assertTrue(stateAfterExport.statusMessage!!.contains("백업 스냅샷이 생성되었습니다"))

        // Clear status
        vm.clearStatusMessage()
        assertNull(vm.uiState.value.statusMessage)
        assertNull(vm.uiState.value.exportedJson)

        // Restore valid snapshot
        val validSnapshot = DatabaseSnapshot(version = 1, exportedAt = 1000L)
        val json = DatabaseBackupEngine.serializeSnapshot(validSnapshot)
        vm.restoreBackup(json)

        val stateAfterRestore = vm.uiState.value
        assertNotNull(stateAfterRestore.statusMessage)
        assertFalse(stateAfterRestore.isStatusError)
        assertTrue(stateAfterRestore.statusMessage!!.contains("성공적으로 복원되었습니다"))

        // Restore invalid snapshot
        vm.restoreBackup("invalid_json")
        val stateAfterError = vm.uiState.value
        assertNotNull(stateAfterError.statusMessage)
        assertTrue(stateAfterError.isStatusError)
        assertTrue(stateAfterError.statusMessage!!.contains("데이터 복원 실패"))
    }
}
