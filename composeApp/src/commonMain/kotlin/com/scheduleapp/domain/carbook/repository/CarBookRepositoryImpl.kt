package com.scheduleapp.domain.carbook.repository

import com.scheduleapp.core.database.dao.CarBookDao
import com.scheduleapp.core.database.entity.CarExpenseLogEntity
import com.scheduleapp.core.database.entity.FuelDetailEntity
import com.scheduleapp.core.database.entity.MaintenanceItemEntity
import com.scheduleapp.core.database.entity.VehicleEntity
import com.scheduleapp.domain.carbook.engine.FuelEfficiencyEngine
import com.scheduleapp.domain.carbook.model.CarExpenseLog
import com.scheduleapp.domain.carbook.model.ExpenseType
import com.scheduleapp.domain.carbook.model.FuelDetail
import com.scheduleapp.domain.carbook.model.FuelRecord
import com.scheduleapp.domain.carbook.model.FuelType
import com.scheduleapp.domain.carbook.model.MaintenanceItem
import com.scheduleapp.domain.carbook.model.Vehicle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class CarBookRepositoryImpl(
    private val carBookDao: CarBookDao
) : CarBookRepository {

    // --- Vehicles ---

    override fun getAllVehicles(): Flow<List<Vehicle>> {
        return carBookDao.getAllVehicles().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getDefaultVehicle(): Flow<Vehicle?> {
        return carBookDao.getDefaultVehicle().map { it?.toDomain() }
    }

    override suspend fun getVehicleById(id: Long): Vehicle? {
        return carBookDao.getVehicleById(id)?.toDomain()
    }

    override suspend fun insertVehicle(vehicle: Vehicle): Long {
        return carBookDao.insertVehicle(vehicle.toEntity())
    }

    override suspend fun updateVehicle(vehicle: Vehicle) {
        carBookDao.updateVehicle(vehicle.toEntity())
    }

    override suspend fun deleteVehicle(vehicle: Vehicle) {
        carBookDao.deleteVehicle(vehicle.toEntity())
    }

    override suspend fun updateVehicleOdometer(vehicleId: Long, newOdometer: Double) {
        val current = carBookDao.getVehicleById(vehicleId)
            ?: throw IllegalArgumentException("Vehicle not found with id: $vehicleId")

        FuelEfficiencyEngine.validateOdometer(current.currentOdometer, newOdometer)
        carBookDao.updateVehicle(current.copy(currentOdometer = newOdometer))
    }

    // --- Car Expense Logs ---

    override fun getAllExpenses(): Flow<List<CarExpenseLog>> {
        return carBookDao.getAllExpenses().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getExpensesForVehicle(vehicleId: Long): Flow<List<CarExpenseLog>> {
        return carBookDao.getExpensesForVehicle(vehicleId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getExpensesForVehicleInRange(
        vehicleId: Long,
        start: Long,
        end: Long
    ): Flow<List<CarExpenseLog>> {
        return carBookDao.getExpensesForVehicleInRange(vehicleId, start, end).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getExpensesByType(vehicleId: Long, type: ExpenseType): Flow<List<CarExpenseLog>> {
        return carBookDao.getExpensesByType(vehicleId, type.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertExpense(expense: CarExpenseLog): Long {
        val vehicle = carBookDao.getVehicleById(expense.vehicleId)
        if (vehicle != null && expense.odometer > vehicle.currentOdometer) {
            carBookDao.updateVehicle(vehicle.copy(currentOdometer = expense.odometer))
        }
        return carBookDao.insertExpense(expense.toEntity())
    }

    override suspend fun updateExpense(expense: CarExpenseLog) {
        val vehicle = carBookDao.getVehicleById(expense.vehicleId)
        if (vehicle != null && expense.odometer > vehicle.currentOdometer) {
            carBookDao.updateVehicle(vehicle.copy(currentOdometer = expense.odometer))
        }
        carBookDao.updateExpense(expense.toEntity())
    }

    override suspend fun deleteExpense(expense: CarExpenseLog) {
        carBookDao.deleteExpense(expense.toEntity())
    }

    override suspend fun deleteExpenseById(id: Long) {
        carBookDao.deleteExpenseById(id)
    }

    // --- Fuel Records & Fuel Details ---

    override fun getFuelRecordsForVehicle(vehicleId: Long): Flow<List<FuelRecord>> {
        return combine(
            carBookDao.getExpensesByType(vehicleId, ExpenseType.FUEL.name),
            carBookDao.getAllFuelDetails()
        ) { expenses, details ->
            val detailsMap = details.associateBy { it.expenseId }
            val rawRecords = expenses.map { expense ->
                val detail = detailsMap[expense.id]
                FuelRecord(
                    expenseId = expense.id,
                    vehicleId = expense.vehicleId,
                    date = expense.date,
                    odometer = expense.odometer,
                    amount = expense.amount,
                    unitPrice = detail?.unitPrice ?: 0.0,
                    liters = detail?.liters ?: 0.0,
                    isFullTank = detail?.isFullTank ?: true,
                    memo = expense.memo
                )
            }
            val processed = FuelEfficiencyEngine.processFuelRecords(rawRecords)
            processed.sortedWith(compareByDescending<FuelRecord> { it.date }.thenByDescending { it.odometer })
        }
    }

    override suspend fun getFuelDetail(expenseId: Long): FuelDetail? {
        return carBookDao.getFuelDetail(expenseId)?.toDomain()
    }

    override suspend fun insertFuelRecord(fuelRecord: FuelRecord): Long {
        val vehicle = carBookDao.getVehicleById(fuelRecord.vehicleId)
        if (vehicle != null) {
            FuelEfficiencyEngine.validateOdometer(vehicle.currentOdometer, fuelRecord.odometer)
        }

        val expenseEntity = CarExpenseLogEntity(
            id = if (fuelRecord.expenseId > 0) fuelRecord.expenseId else 0,
            vehicleId = fuelRecord.vehicleId,
            date = fuelRecord.date,
            type = ExpenseType.FUEL.name,
            amount = fuelRecord.amount,
            odometer = fuelRecord.odometer,
            memo = fuelRecord.memo
        )
        val expenseId = carBookDao.insertExpense(expenseEntity)

        val fuelDetailEntity = FuelDetailEntity(
            expenseId = expenseId,
            unitPrice = fuelRecord.unitPrice,
            liters = fuelRecord.liters,
            isFullTank = fuelRecord.isFullTank,
            calculatedEfficiency = fuelRecord.calculatedEfficiency
        )
        carBookDao.insertFuelDetail(fuelDetailEntity)

        if (vehicle != null && fuelRecord.odometer > vehicle.currentOdometer) {
            carBookDao.updateVehicle(vehicle.copy(currentOdometer = fuelRecord.odometer))
        }

        return expenseId
    }

    override suspend fun updateFuelRecord(fuelRecord: FuelRecord) {
        val expenseEntity = CarExpenseLogEntity(
            id = fuelRecord.expenseId,
            vehicleId = fuelRecord.vehicleId,
            date = fuelRecord.date,
            type = ExpenseType.FUEL.name,
            amount = fuelRecord.amount,
            odometer = fuelRecord.odometer,
            memo = fuelRecord.memo
        )
        carBookDao.updateExpense(expenseEntity)

        val detailEntity = FuelDetailEntity(
            expenseId = fuelRecord.expenseId,
            unitPrice = fuelRecord.unitPrice,
            liters = fuelRecord.liters,
            isFullTank = fuelRecord.isFullTank,
            calculatedEfficiency = fuelRecord.calculatedEfficiency
        )
        carBookDao.updateFuelDetail(detailEntity)
    }

    override suspend fun deleteFuelRecord(expenseId: Long) {
        val detail = carBookDao.getFuelDetail(expenseId)
        if (detail != null) {
            carBookDao.deleteFuelDetail(detail)
        }
        carBookDao.deleteExpenseById(expenseId)
    }

    // --- Entity Mappers ---

    private fun VehicleEntity.toDomain(): Vehicle = Vehicle(
        id = id,
        name = name,
        model = model,
        fuelType = FuelType.fromString(fuelType),
        initialOdometer = initialOdometer,
        currentOdometer = currentOdometer,
        isDefault = isDefault
    )

    private fun Vehicle.toEntity(): VehicleEntity = VehicleEntity(
        id = id,
        name = name,
        model = model,
        fuelType = fuelType.name,
        initialOdometer = initialOdometer,
        currentOdometer = currentOdometer,
        isDefault = isDefault
    )

    private fun CarExpenseLogEntity.toDomain(): CarExpenseLog = CarExpenseLog(
        id = id,
        vehicleId = vehicleId,
        date = date,
        type = ExpenseType.fromString(type),
        amount = amount,
        odometer = odometer,
        memo = memo,
        receiptImageUri = receiptImageUri
    )

    private fun CarExpenseLog.toEntity(): CarExpenseLogEntity = CarExpenseLogEntity(
        id = id,
        vehicleId = vehicleId,
        date = date,
        type = type.name,
        amount = amount,
        odometer = odometer,
        memo = memo,
        receiptImageUri = receiptImageUri
    )

    // --- Maintenance Items ---

    override fun getMaintenanceItemsForVehicle(vehicleId: Long): Flow<List<MaintenanceItem>> {
        return carBookDao.getMaintenanceItemsForVehicle(vehicleId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getMaintenanceItemById(id: Long): MaintenanceItem? {
        return carBookDao.getMaintenanceItemById(id)?.toDomain()
    }

    override suspend fun insertMaintenanceItem(item: MaintenanceItem): Long {
        return carBookDao.insertMaintenanceItem(item.toEntity())
    }

    override suspend fun updateMaintenanceItem(item: MaintenanceItem) {
        carBookDao.updateMaintenanceItem(item.toEntity())
    }

    override suspend fun deleteMaintenanceItem(item: MaintenanceItem) {
        carBookDao.deleteMaintenanceItem(item.toEntity())
    }

    override suspend fun deleteMaintenanceItemById(id: Long) {
        val item = carBookDao.getMaintenanceItemById(id)
        if (item != null) {
            carBookDao.deleteMaintenanceItem(item)
        }
    }

    private fun FuelDetailEntity.toDomain(): FuelDetail = FuelDetail(
        expenseId = expenseId,
        unitPrice = unitPrice,
        liters = liters,
        isFullTank = isFullTank,
        calculatedEfficiency = calculatedEfficiency
    )

    private fun FuelDetail.toEntity(): FuelDetailEntity = FuelDetailEntity(
        expenseId = expenseId,
        unitPrice = unitPrice,
        liters = liters,
        isFullTank = isFullTank,
        calculatedEfficiency = calculatedEfficiency
    )

    private fun MaintenanceItemEntity.toDomain(): MaintenanceItem = MaintenanceItem(
        id = id,
        vehicleId = vehicleId,
        partName = partName,
        cycleKm = cycleKm,
        cycleMonths = cycleMonths,
        lastReplacedDate = lastReplacedDate ?: 0L,
        lastReplacedOdometer = lastReplacedOdometer ?: 0.0,
        nextDueDate = nextDueDate ?: 0L,
        scheduledEventId = scheduledEventId
    )

    private fun MaintenanceItem.toEntity(): MaintenanceItemEntity = MaintenanceItemEntity(
        id = id,
        vehicleId = vehicleId,
        partName = partName,
        cycleKm = cycleKm,
        cycleMonths = cycleMonths,
        lastReplacedDate = lastReplacedDate,
        lastReplacedOdometer = lastReplacedOdometer,
        nextDueDate = nextDueDate,
        scheduledEventId = scheduledEventId
    )
}
