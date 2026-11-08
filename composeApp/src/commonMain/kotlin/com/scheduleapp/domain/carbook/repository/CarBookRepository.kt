package com.scheduleapp.domain.carbook.repository

import com.scheduleapp.domain.carbook.model.CarExpenseLog
import com.scheduleapp.domain.carbook.model.ExpenseType
import com.scheduleapp.domain.carbook.model.FuelDetail
import com.scheduleapp.domain.carbook.model.FuelRecord
import com.scheduleapp.domain.carbook.model.MaintenanceItem
import com.scheduleapp.domain.carbook.model.Vehicle
import kotlinx.coroutines.flow.Flow

interface CarBookRepository {
    // Vehicles
    fun getAllVehicles(): Flow<List<Vehicle>>
    fun getDefaultVehicle(): Flow<Vehicle?>
    suspend fun getVehicleById(id: Long): Vehicle?
    suspend fun insertVehicle(vehicle: Vehicle): Long
    suspend fun updateVehicle(vehicle: Vehicle)
    suspend fun deleteVehicle(vehicle: Vehicle)
    suspend fun updateVehicleOdometer(vehicleId: Long, newOdometer: Double)

    // General Car Expense Logs
    fun getAllExpenses(): Flow<List<CarExpenseLog>>
    fun getExpensesForVehicle(vehicleId: Long): Flow<List<CarExpenseLog>>
    fun getExpensesForVehicleInRange(vehicleId: Long, start: Long, end: Long): Flow<List<CarExpenseLog>>
    fun getExpensesByType(vehicleId: Long, type: ExpenseType): Flow<List<CarExpenseLog>>
    suspend fun insertExpense(expense: CarExpenseLog): Long
    suspend fun updateExpense(expense: CarExpenseLog)
    suspend fun deleteExpense(expense: CarExpenseLog)
    suspend fun deleteExpenseById(id: Long)

    // Fuel Records & Fuel Details
    fun getFuelRecordsForVehicle(vehicleId: Long): Flow<List<FuelRecord>>
    suspend fun getFuelDetail(expenseId: Long): FuelDetail?
    suspend fun insertFuelRecord(fuelRecord: FuelRecord): Long
    suspend fun updateFuelRecord(fuelRecord: FuelRecord)
    suspend fun deleteFuelRecord(expenseId: Long)

    // Maintenance Items
    fun getMaintenanceItemsForVehicle(vehicleId: Long): Flow<List<MaintenanceItem>>
    suspend fun getMaintenanceItemById(id: Long): MaintenanceItem?
    suspend fun insertMaintenanceItem(item: MaintenanceItem): Long
    suspend fun updateMaintenanceItem(item: MaintenanceItem)
    suspend fun deleteMaintenanceItem(item: MaintenanceItem)
    suspend fun deleteMaintenanceItemById(id: Long)
}
