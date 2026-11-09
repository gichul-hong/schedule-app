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
                        initialOdometer = 37000.0,
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
                    val carnivalId = carBookRepository.insertVehicle(primaryVehicle)
                    seedCarnivalHistoryAndMaintenance(carnivalId, preserveOdometer = 114000.0)
                    val cocneyId = carBookRepository.insertVehicle(secondaryVehicle)
                    seedCocneyHistoryAndMaintenance(cocneyId)
                    migrationChecked = true
                    return@collect
                }

                if (!migrationChecked) {
                    migrationChecked = true
                    var migratedAny = false

                    val targetCarnival = vehicles.find {
                        it.name.contains("카니발") || it.name.contains("1호차") || it.model.contains("카니발")
                    }
                    if (targetCarnival != null) {
                        val existingCarnivalExpenses = carBookRepository.getExpensesForVehicle(targetCarnival.id).first()
                        if (existingCarnivalExpenses.isEmpty()) {
                            val finalOdo = maxOf(targetCarnival.currentOdometer, 114000.0)
                            val updatedCarnival = targetCarnival.copy(
                                name = "카니발",
                                model = "카니발 YP (67두 0617)",
                                fuelType = FuelType.DIESEL,
                                initialOdometer = 37000.0,
                                currentOdometer = finalOdo
                            )
                            carBookRepository.updateVehicle(updatedCarnival)
                            seedCarnivalHistoryAndMaintenance(targetCarnival.id, preserveOdometer = finalOdo)
                            migratedAny = true
                        } else {
                            if (!targetCarnival.model.contains("67두")) {
                                carBookRepository.updateVehicle(
                                    targetCarnival.copy(model = "카니발 YP (67두 0617)")
                                )
                            }
                            if (upgradeCarnivalReceiptLogs(targetCarnival.id, existingCarnivalExpenses)) {
                                migratedAny = true
                            }
                        }
                    }

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
                            migratedAny = true
                        }
                    }

                    if (migratedAny) {
                        return@collect
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
        memo: String? = null,
        receiptImageUri: String? = null
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
                    memo = memo,
                    receiptImageUri = receiptImageUri
                )
                carBookRepository.insertExpense(expense)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "지출 기록 추가 중 오류가 발생했습니다.") }
            }
        }
    }

    fun updateExpense(expense: CarExpenseLog) {
        scope.launch {
            try {
                carBookRepository.updateExpense(expense)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "지출 내역 수정 중 오류가 발생했습니다.") }
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

    private suspend fun seedCarnivalHistoryAndMaintenance(
        vehicleId: Long,
        preserveOdometer: Double = 114000.0
    ) {
        fun d(year: Int, month: Int, day: Int): Long =
            DateTimeUtils.startOfDayMillis(LocalDate(year, month, day))

        val carnivalLogs = listOf(
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2017, 8, 1),
                type = ExpenseType.MAINTENANCE,
                amount = 0.0,
                odometer = 37000.0,
                memo = "타이어 4짝 교환"
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2018, 6, 3),
                type = ExpenseType.MAINTENANCE,
                amount = 0.0,
                odometer = 37000.0,
                memo = "엔진오일 구매 (킥스파오1 0W30, 지마켓, 단가 7,220원)"
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2019, 1, 5),
                type = ExpenseType.MAINTENANCE,
                amount = 120000.0,
                odometer = 37000.0,
                memo = "배터리 교체 (델코 90, 삼성화재다이렉트 기사)"
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2019, 3, 6),
                type = ExpenseType.MAINTENANCE,
                amount = 0.0,
                odometer = 50000.0,
                memo = "엔진오일 교체 (킥스파오1 0W30, 11번가, 개당 8,630원)"
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2019, 11, 11),
                type = ExpenseType.MAINTENANCE,
                amount = 53550.0,
                odometer = 59380.0,
                memo = "엔진오일 교체 (킥스파오1 0W30, 11번가 개당 7,650원 × 7통)"
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2020, 8, 25),
                type = ExpenseType.MAINTENANCE,
                amount = 0.0,
                odometer = 59380.0,
                memo = "에어컨필터 교체 (보쉬 S5520 / A2659)"
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2020, 8, 26),
                type = ExpenseType.MAINTENANCE,
                amount = 0.0,
                odometer = 67242.0,
                memo = "엔진오일 교체 (킥스파오1 0W30)"
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2021, 8, 17),
                type = ExpenseType.MAINTENANCE,
                amount = 0.0,
                odometer = 77777.0,
                memo = "엔진오일 교체"
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2022, 4, 29),
                type = ExpenseType.MAINTENANCE,
                amount = 0.0,
                odometer = 84512.0,
                memo = "엔진오일 교체 (킥스파오1 0W30)"
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2022, 5, 7),
                type = ExpenseType.MAINTENANCE,
                amount = 0.0,
                odometer = 84556.0,
                memo = "타이어 위치교환"
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2023, 3, 21),
                type = ExpenseType.MAINTENANCE,
                amount = 0.0,
                odometer = 90059.0,
                memo = "엔진오일 교체"
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2023, 9, 22),
                type = ExpenseType.MAINTENANCE,
                amount = 985000.0,
                odometer = 92716.0,
                memo = "[용인모터스] T/B·인젝터 청소 | 겉벨트 set(31.9만), 흡기/인젝터 크리닝 set(23만), 냉각수 크리닝(7.7만), 부스트압력센서(3.3만), 인젝터 리턴호스(4.4만), #1번 코일(5.5만), 파워오일 울트라(5.5만), 미션오일(15만)",
                receiptImageUri = RECEIPT_20230922_PATH
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2024, 2, 23),
                type = ExpenseType.MAINTENANCE,
                amount = 860000.0,
                odometer = 96967.0,
                memo = "[용인모터스] 엔진오일 공임/필터(4.5만, 오일지참), 앞 로우암 ASSY 좌·우(27.6만), 앞 스테빌 링크 좌·우(5.5만), 엔진/미션 미미 4개 set(26.4만), 리어 트레일링 암 좌·우(22만)",
                receiptImageUri = RECEIPT_20240223_PATH
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2025, 3, 10),
                type = ExpenseType.MAINTENANCE,
                amount = 67900.0,
                odometer = 101200.0,
                memo = "엔진오일 교체 (킥스파오1 0W30)"
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2026, 1, 9),
                type = ExpenseType.MAINTENANCE,
                amount = 0.0,
                odometer = 104800.0,
                memo = "타이어 교환"
            ),
            CarExpenseLog(
                vehicleId = vehicleId,
                date = d(2026, 5, 1),
                type = ExpenseType.MAINTENANCE,
                amount = 0.0,
                odometer = 106591.0,
                memo = "엔진오일 교체"
            )
        )

        carnivalLogs.forEach { log ->
            carBookRepository.insertExpense(log)
        }

        val currentVehicle = carBookRepository.getVehicleById(vehicleId)
        if (currentVehicle != null && currentVehicle.currentOdometer < preserveOdometer) {
            carBookRepository.updateVehicle(currentVehicle.copy(currentOdometer = preserveOdometer))
        }

        val existingMaint = carBookRepository.getMaintenanceItemsForVehicle(vehicleId).first()
        if (existingMaint.isEmpty()) {
            val items = listOf(
                MaintenanceItem(
                    vehicleId = vehicleId,
                    partName = "엔진오일 및 오일필터 (킥스파오1 0W30 / 보쉬 O0096)",
                    cycleKm = 10000.0,
                    cycleMonths = 12,
                    lastReplacedDate = d(2026, 5, 1),
                    lastReplacedOdometer = 106591.0,
                    nextDueDate = MaintenanceWearEngine.calculateNextDueDate(d(2026, 5, 1), 12)
                ),
                MaintenanceItem(
                    vehicleId = vehicleId,
                    partName = "타이어 교체",
                    cycleKm = 50000.0,
                    cycleMonths = 48,
                    lastReplacedDate = d(2026, 1, 9),
                    lastReplacedOdometer = 104800.0,
                    nextDueDate = MaintenanceWearEngine.calculateNextDueDate(d(2026, 1, 9), 48)
                ),
                MaintenanceItem(
                    vehicleId = vehicleId,
                    partName = "에어컨필터 (보쉬 S5520 / A2659)",
                    cycleKm = 15000.0,
                    cycleMonths = 12,
                    lastReplacedDate = d(2020, 8, 25),
                    lastReplacedOdometer = 59380.0,
                    nextDueDate = MaintenanceWearEngine.calculateNextDueDate(d(2020, 8, 25), 12)
                ),
                MaintenanceItem(
                    vehicleId = vehicleId,
                    partName = "배터리 (델코 90)",
                    cycleKm = 60000.0,
                    cycleMonths = 48,
                    lastReplacedDate = d(2019, 1, 5),
                    lastReplacedOdometer = 37000.0,
                    nextDueDate = MaintenanceWearEngine.calculateNextDueDate(d(2019, 1, 5), 48)
                ),
                MaintenanceItem(
                    vehicleId = vehicleId,
                    partName = "미션오일 및 파워오일 (울트라)",
                    cycleKm = 60000.0,
                    cycleMonths = 48,
                    lastReplacedDate = d(2023, 9, 22),
                    lastReplacedOdometer = 92716.0,
                    nextDueDate = MaintenanceWearEngine.calculateNextDueDate(d(2023, 9, 22), 48)
                ),
                MaintenanceItem(
                    vehicleId = vehicleId,
                    partName = "겉벨트 세트 및 냉각수",
                    cycleKm = 80000.0,
                    cycleMonths = 60,
                    lastReplacedDate = d(2023, 9, 22),
                    lastReplacedOdometer = 92716.0,
                    nextDueDate = MaintenanceWearEngine.calculateNextDueDate(d(2023, 9, 22), 60)
                )
            )
            items.forEach { item ->
                if (syncMaintenanceUseCase != null) {
                    syncMaintenanceUseCase.createMaintenanceItem(item)
                } else {
                    carBookRepository.insertMaintenanceItem(item)
                }
            }
        }
    }

    private suspend fun upgradeCarnivalReceiptLogs(
        vehicleId: Long,
        existingLogs: List<CarExpenseLog>
    ): Boolean {
        fun d(year: Int, month: Int, day: Int): Long =
            DateTimeUtils.startOfDayMillis(LocalDate(year, month, day))

        var updated = false
        val date20230922 = d(2023, 9, 22)
        val date20240223 = d(2024, 2, 23)

        val log2023 = existingLogs.find { it.date == date20230922 }
        if (log2023 != null && (log2023.amount < 985000.0 || log2023.receiptImageUri == null)) {
            carBookRepository.updateExpense(
                log2023.copy(
                    amount = 985000.0,
                    odometer = 92716.0,
                    memo = "[용인모터스] T/B·인젝터 청소 | 겉벨트 set(31.9만), 흡기/인젝터 크리닝 set(23만), 냉각수 크리닝(7.7만), 부스트압력센서(3.3만), 인젝터 리턴호스(4.4만), #1번 코일(5.5만), 파워오일 울트라(5.5만), 미션오일(15만)",
                    receiptImageUri = RECEIPT_20230922_PATH
                )
            )
            updated = true
        }

        val log2024 = existingLogs.find { it.date == date20240223 }
        if (log2024 != null && (log2024.amount < 860000.0 || log2024.receiptImageUri == null)) {
            carBookRepository.updateExpense(
                log2024.copy(
                    amount = 860000.0,
                    odometer = 96967.0,
                    memo = "[용인모터스] 엔진오일 공임/필터(4.5만, 오일지참), 앞 로우암 ASSY 좌·우(27.6만), 앞 스테빌 링크 좌·우(5.5만), 엔진/미션 미미 4개 set(26.4만), 리어 트레일링 암 좌·우(22만)",
                    receiptImageUri = RECEIPT_20240223_PATH
                )
            )
            updated = true
        }

        val existingMaint = carBookRepository.getMaintenanceItemsForVehicle(vehicleId).first()
        if (existingMaint.none { it.partName.contains("미션오일") }) {
            val extraItems = listOf(
                MaintenanceItem(
                    vehicleId = vehicleId,
                    partName = "미션오일 및 파워오일 (울트라)",
                    cycleKm = 60000.0,
                    cycleMonths = 48,
                    lastReplacedDate = date20230922,
                    lastReplacedOdometer = 92716.0,
                    nextDueDate = MaintenanceWearEngine.calculateNextDueDate(date20230922, 48)
                ),
                MaintenanceItem(
                    vehicleId = vehicleId,
                    partName = "겉벨트 세트 및 냉각수",
                    cycleKm = 80000.0,
                    cycleMonths = 60,
                    lastReplacedDate = date20230922,
                    lastReplacedOdometer = 92716.0,
                    nextDueDate = MaintenanceWearEngine.calculateNextDueDate(date20230922, 60)
                )
            )
            extraItems.forEach { item ->
                if (syncMaintenanceUseCase != null) {
                    syncMaintenanceUseCase.createMaintenanceItem(item)
                } else {
                    carBookRepository.insertMaintenanceItem(item)
                }
            }
            updated = true
        }

        return updated
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

    companion object {
        const val RECEIPT_20230922_PATH = "/data/user/0/com.scheduleapp/files/receipts/receipt_20230922.jpg"
        const val RECEIPT_20240223_PATH = "/data/user/0/com.scheduleapp/files/receipts/receipt_20240223.png"
    }
}
