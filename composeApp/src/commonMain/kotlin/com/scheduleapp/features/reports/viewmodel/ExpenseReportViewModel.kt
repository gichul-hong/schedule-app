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
import kotlin.math.round

class ExpenseReportViewModel(
    private val carBookRepository: CarBookRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _state = MutableStateFlow(ExpenseReportState(isLoading = true))
    val state: StateFlow<ExpenseReportState> = _state.asStateFlow()

    private var expensesJob: Job? = null
    private var allVehiclesExpenses: List<CarExpenseLog> = emptyList()

    init {
        observeVehicleAndExpenses()
    }

    private fun observeVehicleAndExpenses() {
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

                _state.update { it.copy(vehicles = vehicles, vehicle = currentVehicle) }

                if (expensesJob == null) {
                    observeAllExpenses()
                } else {
                    recalculateReports()
                }
            }
        }
    }

    private fun observeAllExpenses() {
        expensesJob?.cancel()
        expensesJob = scope.launch {
            carBookRepository.getAllExpenses().collect { expenses ->
                allVehiclesExpenses = expenses
                recalculateReports()
            }
        }
    }

    fun selectVehicle(vehicle: Vehicle) {
        _state.update { it.copy(vehicle = vehicle) }
        recalculateReports()
    }

    fun setAllVehiclesMode(allVehicles: Boolean) {
        if (_state.value.isAllVehiclesMode != allVehicles) {
            _state.update { it.copy(isAllVehiclesMode = allVehicles) }
            recalculateReports()
        }
    }

    private fun recalculateReports() {
        val currentState = _state.value
        val currentYear = currentState.selectedYear
        val currentMonth = currentState.selectedMonth
        val selectedVehicle = currentState.vehicle

        val targetExpenses = if (currentState.isAllVehiclesMode || selectedVehicle == null) {
            allVehiclesExpenses
        } else {
            allVehiclesExpenses.filter { it.vehicleId == selectedVehicle.id }
        }

        val availableYears = ExpenseReportEngine.getAvailableYears(allVehiclesExpenses, DateTimeUtils.today().year)
        val trend = ExpenseReportEngine.generateMonthlyTrend(targetExpenses, currentYear)
        val breakdown = ExpenseReportEngine.generateCategoryBreakdown(targetExpenses, currentYear, currentMonth)
        val summary = ExpenseReportEngine.generateSummary(targetExpenses, currentYear, currentMonth)

        // Calculate per-vehicle comparison for the selected period (Year / Month)
        val periodAllVehicleExpenses = allVehiclesExpenses.filter { log ->
            val date = DateTimeUtils.localDateFromMillis(log.date)
            date.year == currentYear && (currentMonth == null || date.monthNumber == currentMonth)
        }
        val totalAllVehiclesPeriodSpend = periodAllVehicleExpenses.sumOf { it.amount }
        val comparison = currentState.vehicles.map { v ->
            val vSpend = periodAllVehicleExpenses.filter { it.vehicleId == v.id }.sumOf { it.amount }
            val pct = if (totalAllVehiclesPeriodSpend > 0.0) {
                round((vSpend / totalAllVehiclesPeriodSpend) * 1000.0) / 10.0
            } else {
                0.0
            }
            VehicleSpendShare(vehicle = v, amount = vSpend, percentage = pct)
        }

        _state.update {
            it.copy(
                availableYears = availableYears,
                monthlyTrend = trend,
                categoryBreakdown = breakdown,
                summary = summary,
                vehicleSpendComparison = comparison,
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
