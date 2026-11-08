package com.scheduleapp.features.maintenance.viewmodel

import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.carbook.engine.MaintenanceWearEngine
import com.scheduleapp.domain.carbook.model.MaintenanceItem
import com.scheduleapp.domain.carbook.model.MaintenancePreset
import com.scheduleapp.domain.carbook.model.MaintenanceStatus
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MaintenanceViewModel(
    private val carBookRepository: CarBookRepository,
    private val syncMaintenanceUseCase: SyncMaintenanceToScheduleUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _state = MutableStateFlow(MaintenanceState(isLoading = true))
    val state: StateFlow<MaintenanceState> = _state.asStateFlow()

    private var itemsJob: Job? = null

    init {
        observeVehicleAndItems()
    }

    private fun observeVehicleAndItems() {
        scope.launch {
            carBookRepository.getAllVehicles().collect { vehicles ->
                if (vehicles.isEmpty()) {
                    _state.update { it.copy(vehicles = emptyList(), vehicle = null, isLoading = false) }
                    return@collect
                }

                val selectedId = _state.value.vehicle?.id
                val currentVehicle = vehicles.find { it.id == selectedId }
                    ?: vehicles.firstOrNull { it.isDefault }
                    ?: vehicles.first()
                val vehicleChanged = _state.value.vehicle?.id != currentVehicle.id ||
                    _state.value.vehicle?.currentOdometer != currentVehicle.currentOdometer

                _state.update { it.copy(vehicles = vehicles, vehicle = currentVehicle) }

                if (vehicleChanged || itemsJob == null) {
                    observeItems(currentVehicle)
                }
            }
        }
    }

    fun selectVehicle(vehicle: Vehicle) {
        if (_state.value.vehicle?.id != vehicle.id || _state.value.vehicle?.currentOdometer != vehicle.currentOdometer) {
            _state.update { it.copy(vehicle = vehicle) }
            observeItems(vehicle)
        }
    }

    private fun observeItems(vehicle: Vehicle) {
        itemsJob?.cancel()
        itemsJob = scope.launch {
            carBookRepository.getMaintenanceItemsForVehicle(vehicle.id).collect { rawItems ->
                val now = DateTimeUtils.nowMillis()
                val itemsWithWear = rawItems.map { item ->
                    val wear = MaintenanceWearEngine.calculateDetailedWear(
                        item = item,
                        currentOdometer = vehicle.currentOdometer,
                        currentDateMillis = now
                    )
                    MaintenanceItemWithWear(item, wear)
                }.sortedWith(
                    compareByDescending<MaintenanceItemWithWear> { it.wearResult.wearPercentage }
                )

                val overdue = itemsWithWear.count { it.wearResult.status == MaintenanceStatus.OVERDUE }
                val warning = itemsWithWear.count { it.wearResult.status == MaintenanceStatus.WARNING }
                val ok = itemsWithWear.count { it.wearResult.status == MaintenanceStatus.OK }

                _state.update {
                    it.copy(
                        items = itemsWithWear,
                        overdueCount = overdue,
                        warningCount = warning,
                        okCount = ok,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun addMaintenanceItem(
        partName: String,
        cycleKm: Double,
        cycleMonths: Int,
        lastReplacedDate: Long = DateTimeUtils.nowMillis(),
        lastReplacedOdometer: Double? = null
    ) {
        val vehicle = _state.value.vehicle ?: return
        scope.launch {
            try {
                val odo = lastReplacedOdometer ?: vehicle.currentOdometer
                val nextDueDate = MaintenanceWearEngine.calculateNextDueDate(lastReplacedDate, cycleMonths)
                val newItem = MaintenanceItem(
                    vehicleId = vehicle.id,
                    partName = partName,
                    cycleKm = cycleKm,
                    cycleMonths = cycleMonths,
                    lastReplacedDate = lastReplacedDate,
                    lastReplacedOdometer = odo,
                    nextDueDate = nextDueDate
                )
                syncMaintenanceUseCase.createMaintenanceItem(newItem)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "소모품 등록 중 오류가 발생했습니다.") }
            }
        }
    }

    fun addPresetItem(preset: MaintenancePreset) {
        val vehicle = _state.value.vehicle ?: return
        addMaintenanceItem(
            partName = preset.partName,
            cycleKm = preset.cycleKm,
            cycleMonths = preset.cycleMonths,
            lastReplacedDate = DateTimeUtils.nowMillis(),
            lastReplacedOdometer = vehicle.currentOdometer
        )
    }

    fun seedDefaultPresetsIfEmpty() {
        val vehicle = _state.value.vehicle ?: return
        if (_state.value.items.isNotEmpty()) return

        scope.launch {
            try {
                for (preset in MaintenancePreset.DEFAULT_PRESETS) {
                    addMaintenanceItem(
                        partName = preset.partName,
                        cycleKm = preset.cycleKm,
                        cycleMonths = preset.cycleMonths,
                        lastReplacedDate = DateTimeUtils.nowMillis(),
                        lastReplacedOdometer = vehicle.currentOdometer
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "기본 소모품 추가 중 오류가 발생했습니다.") }
            }
        }
    }

    fun logServiceReplacement(
        itemId: Long,
        serviceDate: Long,
        serviceOdometer: Double,
        cost: Double,
        memo: String? = null
    ) {
        scope.launch {
            try {
                syncMaintenanceUseCase.logMaintenanceReplacement(
                    itemId = itemId,
                    serviceDate = serviceDate,
                    serviceOdometer = serviceOdometer,
                    cost = cost,
                    memo = memo
                )
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "정비 완료 기록 중 오류가 발생했습니다.") }
            }
        }
    }

    fun deleteMaintenanceItem(itemId: Long) {
        scope.launch {
            try {
                syncMaintenanceUseCase.deleteMaintenanceItem(itemId)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "소모품 삭제 중 오류가 발생했습니다.") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
