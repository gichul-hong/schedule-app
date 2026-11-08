package com.scheduleapp.features.maintenance.viewmodel

import com.scheduleapp.domain.carbook.engine.MaintenanceWearResult
import com.scheduleapp.domain.carbook.model.MaintenanceItem
import com.scheduleapp.domain.carbook.model.Vehicle

data class MaintenanceItemWithWear(
    val item: MaintenanceItem,
    val wearResult: MaintenanceWearResult
)

data class MaintenanceState(
    val vehicle: Vehicle? = null,
    val items: List<MaintenanceItemWithWear> = emptyList(),
    val overdueCount: Int = 0,
    val warningCount: Int = 0,
    val okCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
