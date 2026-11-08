package com.scheduleapp.features.reports.viewmodel

import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.carbook.engine.ExpenseReportEngine
import com.scheduleapp.domain.carbook.model.CarExpenseLog
import com.scheduleapp.domain.carbook.model.Vehicle
import com.scheduleapp.domain.carbook.repository.CarBookRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExpenseReportViewModel(
    private val carBookRepository: CarBookRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _state = MutableStateFlow(ExpenseReportState(isLoading = true))
    val state: StateFlow<ExpenseReportState> = _state.asStateFlow()

    private var expensesJob: Job? = null
    private var allExpenses: List<CarExpenseLog> = emptyList()

    init {
        observeVehicleAndExpenses()
    }

    private fun observeVehicleAndExpenses() {
        scope.launch {
            carBookRepository.getAllVehicles().collect { vehicles ->
                if (vehicles.isEmpty()) {
                    _state.update { it.copy(isLoading = false) }
                    return@collect
                }

                val currentVehicle = vehicles.firstOrNull { it.isDefault } ?: vehicles.first()
                val vehicleChanged = _state.value.vehicle?.id != currentVehicle.id

                _state.update { it.copy(vehicle = currentVehicle) }

                if (vehicleChanged || expensesJob == null) {
                    observeExpenses(currentVehicle)
                }
            }
        }
    }

    private fun observeExpenses(vehicle: Vehicle) {
        expensesJob?.cancel()
        expensesJob = scope.launch {
            carBookRepository.getExpensesForVehicle(vehicle.id).collect { expenses ->
                allExpenses = expenses
                recalculateReports()
            }
        }
    }

    private fun recalculateReports() {
        val currentYear = _state.value.selectedYear
        val currentMonth = _state.value.selectedMonth

        val availableYears = ExpenseReportEngine.getAvailableYears(allExpenses, DateTimeUtils.today().year)
        val trend = ExpenseReportEngine.generateMonthlyTrend(allExpenses, currentYear)
        val breakdown = ExpenseReportEngine.generateCategoryBreakdown(allExpenses, currentYear, currentMonth)
        val summary = ExpenseReportEngine.generateSummary(allExpenses, currentYear, currentMonth)

        _state.update {
            it.copy(
                availableYears = availableYears,
                monthlyTrend = trend,
                categoryBreakdown = breakdown,
                summary = summary,
                isLoading = false
            )
        }
    }

    fun selectYear(year: Int) {
        if (_state.value.selectedYear != year) {
            _state.update { it.copy(selectedYear = year) }
            recalculateReports()
        }
    }

    fun selectMonth(month: Int?) {
        if (_state.value.selectedMonth != month) {
            _state.update { it.copy(selectedMonth = month) }
            recalculateReports()
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
