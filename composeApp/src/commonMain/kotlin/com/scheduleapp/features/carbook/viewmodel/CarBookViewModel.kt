package com.scheduleapp.features.carbook.viewmodel

import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.carbook.engine.FuelEfficiencyEngine
import com.scheduleapp.domain.carbook.model.CarExpenseLog
import com.scheduleapp.domain.carbook.model.ExpenseType
import com.scheduleapp.domain.carbook.model.FuelRecord
import com.scheduleapp.domain.carbook.model.FuelType
import com.scheduleapp.domain.carbook.model.Vehicle
import com.scheduleapp.domain.carbook.repository.CarBookRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CarBookViewModel(
    private val carBookRepository: CarBookRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _state = MutableStateFlow(CarBookState(isLoading = true))
    val state: StateFlow<CarBookState> = _state.asStateFlow()

    private var vehicleDataJob: Job? = null

    init {
        observeVehicles()
    }

    private fun observeVehicles() {
        scope.launch {
            carBookRepository.getAllVehicles().collect { vehicles ->
                if (vehicles.isEmpty()) {
                    // Seed initial default vehicle for new users
                    val defaultVehicle = Vehicle(
                        name = "내 차량",
                        model = "아반떼 CN7",
                        fuelType = FuelType.GASOLINE,
                        initialOdometer = 10000.0,
                        currentOdometer = 10000.0,
                        isDefault = true
                    )
                    carBookRepository.insertVehicle(defaultVehicle)
                    return@collect
                }

                val currentSelectedId = _state.value.selectedVehicle?.id
                val matched = vehicles.find { it.id == currentSelectedId }
                    ?: vehicles.firstOrNull { it.isDefault }
                    ?: vehicles.first()

                val shouldRestartObserver = (_state.value.selectedVehicle?.id != matched.id) || vehicleDataJob == null

                _state.update {
                    it.copy(
                        vehicles = vehicles,
                        selectedVehicle = matched,
                        totalDrivenKm = (matched.currentOdometer - matched.initialOdometer).coerceAtLeast(0.0),
                        isLoading = false
                    )
                }

                if (shouldRestartObserver) {
                    observeVehicleData(matched.id)
                }
            }
        }
    }

    private fun observeVehicleData(vehicleId: Long) {
        vehicleDataJob?.cancel()
        vehicleDataJob = scope.launch {
            combine(
                carBookRepository.getFuelRecordsForVehicle(vehicleId),
                carBookRepository.getExpensesForVehicle(vehicleId)
            ) { fuelRecords, expenseLogs ->
                val avgKmPerL = FuelEfficiencyEngine.calculateCumulativeAverageEfficiency(fuelRecords)
                val avgCostPerKm = FuelEfficiencyEngine.calculateCumulativeAverageCostPerKm(fuelRecords)
                val totalFuel = fuelRecords.sumOf { it.amount }
                val totalExpense = expenseLogs.sumOf { it.amount }

                _state.update { current ->
                    current.copy(
                        fuelRecords = fuelRecords,
                        expenseLogs = expenseLogs,
                        averageKmPerL = avgKmPerL,
                        costPerKm = avgCostPerKm,
                        totalFuelCost = totalFuel,
                        totalExpenseCost = totalExpense,
                        isLoading = false
                    )
                }
            }.collect {}
        }
    }

    fun selectVehicle(vehicle: Vehicle) {
        if (_state.value.selectedVehicle?.id != vehicle.id) {
            _state.update {
                it.copy(
                    selectedVehicle = vehicle,
                    totalDrivenKm = (vehicle.currentOdometer - vehicle.initialOdometer).coerceAtLeast(0.0)
                )
            }
            observeVehicleData(vehicle.id)
        }
    }

    fun setTab(tab: CarBookTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun addFuelRecord(
        amount: Double,
        liters: Double,
        unitPrice: Double,
        odometer: Double,
        isFullTank: Boolean,
        date: Long = DateTimeUtils.nowMillis(),
        memo: String? = null
    ) {
        val vehicle = _state.value.selectedVehicle ?: return
        scope.launch {
            try {
                val record = FuelRecord(
                    vehicleId = vehicle.id,
                    date = date,
                    odometer = odometer,
                    amount = amount,
                    unitPrice = unitPrice,
                    liters = liters,
                    isFullTank = isFullTank,
                    memo = memo
                )
                carBookRepository.insertFuelRecord(record)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "주유 기록 추가 중 오류가 발생했습니다.") }
            }
        }
    }

    fun addExpense(
        type: ExpenseType,
        amount: Double,
        odometer: Double?,
        date: Long = DateTimeUtils.nowMillis(),
        memo: String? = null
    ) {
        val vehicle = _state.value.selectedVehicle ?: return
        scope.launch {
            try {
                val finalOdometer = odometer ?: vehicle.currentOdometer
                val expense = CarExpenseLog(
                    vehicleId = vehicle.id,
                    date = date,
                    type = type,
                    amount = amount,
                    odometer = finalOdometer,
                    memo = memo
                )
                carBookRepository.insertExpense(expense)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "지출 기록 추가 중 오류가 발생했습니다.") }
            }
        }
    }

    fun updateVehicleOdometer(newOdometer: Double) {
        val vehicle = _state.value.selectedVehicle ?: return
        scope.launch {
            try {
                carBookRepository.updateVehicleOdometer(vehicle.id, newOdometer)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "주행거리 변경 오류") }
            }
        }
    }

    fun saveVehicleProfile(vehicle: Vehicle) {
        scope.launch {
            try {
                if (vehicle.id == 0L) {
                    val newId = carBookRepository.insertVehicle(vehicle)
                    val inserted = carBookRepository.getVehicleById(newId)
                    if (inserted != null) {
                        selectVehicle(inserted)
                    }
                } else {
                    carBookRepository.updateVehicle(vehicle)
                    _state.update { it.copy(selectedVehicle = vehicle) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "차량 정보 저장 중 오류가 발생했습니다.") }
            }
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        scope.launch {
            try {
                carBookRepository.deleteVehicle(vehicle)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "차량 삭제 중 오류가 발생했습니다.") }
            }
        }
    }

    fun deleteFuelRecord(expenseId: Long) {
        scope.launch {
            try {
                carBookRepository.deleteFuelRecord(expenseId)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "주유 기록 삭제 중 오류가 발생했습니다.") }
            }
        }
    }

    fun deleteExpense(expense: CarExpenseLog) {
        scope.launch {
            try {
                carBookRepository.deleteExpense(expense)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "지출 기록 삭제 중 오류가 발생했습니다.") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
