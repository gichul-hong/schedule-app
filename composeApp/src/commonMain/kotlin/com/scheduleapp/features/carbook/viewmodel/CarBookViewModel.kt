package com.scheduleapp.features.carbook.viewmodel

import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.carbook.engine.FuelEfficiencyEngine
import com.scheduleapp.domain.carbook.engine.MaintenanceWearEngine
import com.scheduleapp.domain.carbook.model.CarExpenseLog
import com.scheduleapp.domain.carbook.model.ExpenseType
import com.scheduleapp.domain.carbook.model.FuelRecord
import com.scheduleapp.domain.carbook.model.FuelType
import com.scheduleapp.domain.carbook.model.MaintenanceItem
import com.scheduleapp.domain.carbook.model.Vehicle
import com.scheduleapp.domain.carbook.repository.CarBookRepository
import com.scheduleapp.domain.carbook.usecase.SyncMaintenanceToScheduleUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class CarBookViewModel(
    private val carBookRepository: CarBookRepository,
    private val syncMaintenanceUseCase: SyncMaintenanceToScheduleUseCase? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _state = MutableStateFlow(CarBookState(isLoading = true))
    val state: StateFlow<CarBookState> = _state.asStateFlow()

    private var vehicleDataJob: Job? = null
    private var migrationChecked = false

    init {
        observeVehicles()
    }

    private fun observeVehicles() {
        scope.launch {
            carBookRepository.getAllVehicles().collect { vehicles ->
                if (vehicles.isEmpty()) {
                    // Seed 2 initial vehicles for 2-car ownership out of the box
                    val primaryVehicle = Vehicle(
                        name = "카니발",
                        model = "카니발",
                        fuelType = FuelType.DIESEL,
                        initialOdometer = 110000.0,
                        currentOdometer = 114000.0,
                        isDefault = true
                    )
                    val secondaryVehicle = Vehicle(
                        name = "코크니",
                        model = "BMW iX3 M Spt LCI",
                        fuelType = FuelType.ELECTRIC,
                        initialOdometer = 9500.0,
                        currentOdometer = 9500.0,
                        isDefault = false
                    )
                    carBookRepository.insertVehicle(primaryVehicle)
                    val cocneyId = carBookRepository.insertVehicle(secondaryVehicle)
                    seedCocneyHistoryAndMaintenance(cocneyId)
                    migrationChecked = true
                    return@collect
                }

                if (!migrationChecked) {
                    migrationChecked = true
                    val targetCocney = vehicles.find {
                        it.name.contains("코크니") || it.name.contains("2호차") || it.model.contains("ix3", ignoreCase = true)
                    }
                    if (targetCocney != null) {
                        val existingExpenses = carBookRepository.getExpensesForVehicle(targetCocney.id).first()
                        if (existingExpenses.isEmpty()) {
                            val updatedCocney = targetCocney.copy(
                                name = "코크니",
                                model = "BMW iX3 M Spt LCI",
                                fuelType = FuelType.ELECTRIC,
                                initialOdometer = 9500.0,
                                currentOdometer = 9500.0
                            )
                            carBookRepository.updateVehicle(updatedCocney)
                            seedCocneyHistoryAndMaintenance(targetCocney.id)
                            return@collect
                        }
                    }
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
                if (vehicle.isDefault) {
                    _state.value.vehicles.filter { it.isDefault && it.id != vehicle.id }.forEach { other ->
                        carBookRepository.updateVehicle(other.copy(isDefault = false))
                    }
                }
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

    private suspend fun seedCocneyHistoryAndMaintenance(vehicleId: Long) {
        fun d(year: Int, month: Int, day: Int): Long =
            DateTimeUtils.startOfDayMillis(LocalDate(year, month, day))

        val historicalLogs = listOf(
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2022, 11, 17),
                type = ExpenseType.MAINTENANCE,
                amount = 0.0,
                odometer = 9500.0,
                memo = "윈터 타이어로 교체 (시즌 교체)"
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2022, 12, 26),
                type = ExpenseType.MAINTENANCE,
                amount = 0.0,
                odometer = 9500.0,
                memo = "좌측 뒤 타이어 펑크 수리 (버섯패치)"
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2023, 3, 26),
                type = ExpenseType.MAINTENANCE,
                amount = 0.0,
                odometer = 13300.0,
                memo = "썸머 타이어로 교체 (시즌 교체)"
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2024, 3, 10),
                type = ExpenseType.MAINTENANCE,
                amount = 0.0,
                odometer = 23133.0,
                memo = "썸머 타이어로 교체 (시즌 교체)"
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2025, 3, 8),
                type = ExpenseType.MAINTENANCE,
                amount = 0.0,
                odometer = 31444.0,
                memo = "썸머 타이어로 교체 + 휠 얼라인먼트"
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2025, 11, 13),
                type = ExpenseType.MAINTENANCE,
                amount = 0.0,
                odometer = 39700.0,
                memo = "윈터 타이어로 교체 (Winter 시즌 교체)"
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2026, 3, 15),
                type = ExpenseType.MAINTENANCE,
                amount = 0.0,
                odometer = 43292.0,
                memo = "썸머 타이어로 교체 (Summer 시즌 교체)"
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2026, 11, 9),
                type = ExpenseType.MAINTENANCE,
                amount = 690000.0,
                odometer = 52820.0,
                memo = "윈터 타이어 4짝 교체 (금호 윈터크래프트 WP72 | 앞 245/45R20 / 뒤 275/40R20) | 차대번호: WBY41DU06NS276760"
            )
        )

        historicalLogs.forEach { log ->
            carBookRepository.insertExpense(log)
        }

        val existingMaint = carBookRepository.getMaintenanceItemsForVehicle(vehicleId).first()
        if (existingMaint.isEmpty()) {
            val lastWinterSwap = d(2026, 11, 9)
            val nextSummerSwap = MaintenanceWearEngine.calculateNextDueDate(lastWinterSwap, 4)
            val tireItem = MaintenanceItem(
                vehicleId = vehicleId,
                partName = "시즌 타이어 교체 (윈터→썸머 | 앞245/45R20 뒤275/40R20)",
                cycleKm = 8000.0,
                cycleMonths = 4,
                lastReplacedDate = lastWinterSwap,
                lastReplacedOdometer = 52820.0,
                nextDueDate = nextSummerSwap
            )
            if (syncMaintenanceUseCase != null) {
                syncMaintenanceUseCase.createMaintenanceItem(tireItem)
            } else {
                carBookRepository.insertMaintenanceItem(tireItem)
            }

            val lastAlignDate = d(2025, 3, 8)
            val nextAlignDate = MaintenanceWearEngine.calculateNextDueDate(lastAlignDate, 24)
            val alignItem = MaintenanceItem(
                vehicleId = vehicleId,
                partName = "휠 얼라인먼트 점검",
                cycleKm = 25000.0,
                cycleMonths = 24,
                lastReplacedDate = lastAlignDate,
                lastReplacedOdometer = 31444.0,
                nextDueDate = nextAlignDate
            )
            if (syncMaintenanceUseCase != null) {
                syncMaintenanceUseCase.createMaintenanceItem(alignItem)
            } else {
                carBookRepository.insertMaintenanceItem(alignItem)
            }
        }
    }
}
