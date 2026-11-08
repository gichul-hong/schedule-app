package com.scheduleapp.features.reports.viewmodel

import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.carbook.engine.ExpenseCategorySlice
import com.scheduleapp.domain.carbook.engine.ExpenseReportSummary
import com.scheduleapp.domain.carbook.engine.MonthlyExpenseBarData
import com.scheduleapp.domain.carbook.model.Vehicle

data class VehicleSpendShare(
    val vehicle: Vehicle,
    val amount: Double,
    val percentage: Double
)

data class ExpenseReportState(
    val vehicles: List<Vehicle> = emptyList(),
    val vehicle: Vehicle? = null,
    val isAllVehiclesMode: Boolean = false,
    val vehicleSpendComparison: List<VehicleSpendShare> = emptyList(),
    val selectedYear: Int = DateTimeUtils.today().year,
    val selectedMonth: Int? = null, // null for full year, 1..12 for specific month
    val availableYears: List<Int> = listOf(DateTimeUtils.today().year),
    val monthlyTrend: List<MonthlyExpenseBarData> = emptyList(),
    val categoryBreakdown: List<ExpenseCategorySlice> = emptyList(),
    val summary: ExpenseReportSummary = ExpenseReportSummary(0.0, 0.0, null, 0.0, 0.0, 0.0, 0.0),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
