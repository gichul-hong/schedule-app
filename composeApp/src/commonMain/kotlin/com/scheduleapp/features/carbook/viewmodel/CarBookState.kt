package com.scheduleapp.features.carbook.viewmodel

import com.scheduleapp.domain.carbook.model.CarExpenseLog
import com.scheduleapp.domain.carbook.model.FuelRecord
import com.scheduleapp.domain.carbook.model.Vehicle

enum class CarBookTab(val title: String) {
    FUEL("주유 기록"),
    MAINTENANCE("정비/소모품"),
    EXPENSE("지출 내역"),
    REPORT("유지비 리포트")
}

data class CarBookState(
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicle: Vehicle? = null,
    val fuelRecords: List<FuelRecord> = emptyList(),
    val expenseLogs: List<CarExpenseLog> = emptyList(),
    val selectedTab: CarBookTab = CarBookTab.FUEL,
    val averageKmPerL: Double? = null,
    val costPerKm: Double? = null,
    val totalFuelCost: Double = 0.0,
    val totalExpenseCost: Double = 0.0,
    val totalDrivenKm: Double = 0.0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
