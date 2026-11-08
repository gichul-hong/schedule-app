package com.scheduleapp.core.database.dao

import androidx.room.*
import com.scheduleapp.core.database.entity.CarExpenseLogEntity
import com.scheduleapp.core.database.entity.FuelDetailEntity
import com.scheduleapp.core.database.entity.MaintenanceItemEntity
import com.scheduleapp.core.database.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CarBookDao {
    // Vehicles
    @Query("SELECT * FROM vehicles ORDER BY id ASC")
    fun getAllVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE isDefault = 1 LIMIT 1")
    fun getDefaultVehicle(): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getVehicleById(id: Long): VehicleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity): Long

    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity)

    @Delete
    suspend fun deleteVehicle(vehicle: VehicleEntity)

    // Car Expense Logs
    @Query("SELECT * FROM car_expense_logs WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getExpensesForVehicle(vehicleId: Long): Flow<List<CarExpenseLogEntity>>

    @Query("SELECT * FROM car_expense_logs WHERE vehicleId = :vehicleId AND date >= :start AND date <= :end ORDER BY date DESC")
    fun getExpensesForVehicleInRange(vehicleId: Long, start: Long, end: Long): Flow<List<CarExpenseLogEntity>>

    @Query("SELECT * FROM car_expense_logs WHERE vehicleId = :vehicleId AND type = :type ORDER BY date DESC")
    fun getExpensesByType(vehicleId: Long, type: String): Flow<List<CarExpenseLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: CarExpenseLogEntity): Long

    @Update
    suspend fun updateExpense(expense: CarExpenseLogEntity)

    @Query("SELECT * FROM car_expense_logs WHERE id = :id")
    suspend fun getExpenseById(id: Long): CarExpenseLogEntity?

    @Query("DELETE FROM car_expense_logs WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Delete
    suspend fun deleteExpense(expense: CarExpenseLogEntity)

    // Fuel Details
    @Query("SELECT * FROM fuel_details")
    fun getAllFuelDetails(): Flow<List<FuelDetailEntity>>

    @Query("SELECT * FROM fuel_details WHERE expenseId = :expenseId")
    suspend fun getFuelDetail(expenseId: Long): FuelDetailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelDetail(fuelDetail: FuelDetailEntity)

    @Update
    suspend fun updateFuelDetail(fuelDetail: FuelDetailEntity)

    @Delete
    suspend fun deleteFuelDetail(fuelDetail: FuelDetailEntity)

    // Maintenance Items
    @Query("SELECT * FROM maintenance_items WHERE vehicleId = :vehicleId ORDER BY id ASC")
    fun getMaintenanceItemsForVehicle(vehicleId: Long): Flow<List<MaintenanceItemEntity>>

    @Query("SELECT * FROM maintenance_items WHERE id = :id")
    suspend fun getMaintenanceItemById(id: Long): MaintenanceItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenanceItem(item: MaintenanceItemEntity): Long

    @Update
    suspend fun updateMaintenanceItem(item: MaintenanceItemEntity)

    @Delete
    suspend fun deleteMaintenanceItem(item: MaintenanceItemEntity)

    // Bulk & Global Queries for Backup & Restore
    @Query("SELECT * FROM car_expense_logs ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<CarExpenseLogEntity>>

    @Query("SELECT * FROM maintenance_items ORDER BY id ASC")
    fun getAllMaintenanceItems(): Flow<List<MaintenanceItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(vehicles: List<VehicleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<CarExpenseLogEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelDetails(details: List<FuelDetailEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenanceItems(items: List<MaintenanceItemEntity>)

    @Query("DELETE FROM fuel_details")
    suspend fun clearAllFuelDetails()

    @Query("DELETE FROM car_expense_logs")
    suspend fun clearAllExpenses()

    @Query("DELETE FROM maintenance_items")
    suspend fun clearAllMaintenanceItems()

    @Query("DELETE FROM vehicles")
    suspend fun clearAllVehicles()
}
