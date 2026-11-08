package com.scheduleapp.domain.carbook.engine

import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.carbook.model.CarExpenseLog
import com.scheduleapp.domain.carbook.model.ExpenseType
import kotlin.math.round

data class MonthlyExpenseBarData(
    val year: Int,
    val month: Int,
    val label: String,
    val totalAmount: Double,
    val fuelAmount: Double,
    val maintenanceAmount: Double,
    val otherAmount: Double
)

data class ExpenseCategorySlice(
    val type: ExpenseType,
    val amount: Double,
    val percentage: Double,
    val colorHex: Long
)

data class ExpenseReportSummary(
    val totalSpend: Double,
    val monthlyAverage: Double,
    val largestCategory: ExpenseType?,
    val largestCategoryAmount: Double,
    val fuelTotal: Double,
    val maintenanceTotal: Double,
    val fixedCostsTotal: Double
)

object ExpenseReportEngine {

    fun getCategoryColorHex(type: ExpenseType): Long {
        return when (type) {
            ExpenseType.FUEL -> 0xFFFFA000L        // Amber / Orange
            ExpenseType.MAINTENANCE -> 0xFF1976D2L // Blue
            ExpenseType.PARKING -> 0xFF7B1FA2L     // Purple
            ExpenseType.TOLL -> 0xFF00897BL        // Teal
            ExpenseType.INSURANCE -> 0xFFD32F2FL   // Crimson Red
            ExpenseType.TAX -> 0xFF3949ABL         // Indigo
            ExpenseType.WASH -> 0xFF0288D1L        // Light Blue
            ExpenseType.OTHER -> 0xFF757575L       // Grey
        }
    }

    /**
     * Generates a 12-month expense trend for the specified year.
     */
    fun generateMonthlyTrend(
        expenses: List<CarExpenseLog>,
        targetYear: Int
    ): List<MonthlyExpenseBarData> {
        val expensesByMonth = expenses
            .filter {
                val date = DateTimeUtils.localDateFromMillis(it.date)
                date.year == targetYear
            }
            .groupBy { DateTimeUtils.localDateFromMillis(it.date).monthNumber }

        return (1..12).map { month ->
            val monthExpenses = expensesByMonth[month] ?: emptyList()
            val fuelAmount = monthExpenses.filter { it.type == ExpenseType.FUEL }.sumOf { it.amount }
            val maintenanceAmount = monthExpenses.filter { it.type == ExpenseType.MAINTENANCE }.sumOf { it.amount }
            val otherAmount = monthExpenses.filter { it.type != ExpenseType.FUEL && it.type != ExpenseType.MAINTENANCE }.sumOf { it.amount }
            val totalAmount = fuelAmount + maintenanceAmount + otherAmount

            MonthlyExpenseBarData(
                year = targetYear,
                month = month,
                label = "${month}월",
                totalAmount = totalAmount,
                fuelAmount = fuelAmount,
                maintenanceAmount = maintenanceAmount,
                otherAmount = otherAmount
            )
        }
    }

    /**
     * Generates category breakdown slices with percentages and colors for donut charts.
     */
    fun generateCategoryBreakdown(
        expenses: List<CarExpenseLog>,
        targetYear: Int,
        targetMonth: Int? = null
    ): List<ExpenseCategorySlice> {
        val filtered = filterExpenses(expenses, targetYear, targetMonth)
        val total = filtered.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        val grouped = filtered.groupBy { it.type }

        return ExpenseType.entries.mapNotNull { type ->
            val list = grouped[type] ?: emptyList()
            val amount = list.sumOf { it.amount }
            if (amount > 0.0) {
                val percentage = round((amount / total) * 1000.0) / 10.0
                ExpenseCategorySlice(
                    type = type,
                    amount = amount,
                    percentage = percentage,
                    colorHex = getCategoryColorHex(type)
                )
            } else null
        }.sortedByDescending { it.amount }
    }

    /**
     * Computes the summary statistics for the target year (and optional month).
     */
    fun generateSummary(
        expenses: List<CarExpenseLog>,
        targetYear: Int,
        targetMonth: Int? = null
    ): ExpenseReportSummary {
        val filtered = filterExpenses(expenses, targetYear, targetMonth)
        val totalSpend = filtered.sumOf { it.amount }

        val monthlyAverage = if (targetMonth != null) {
            totalSpend
        } else {
            val today = DateTimeUtils.today()
            val monthsCount = if (targetYear == today.year) {
                today.monthNumber.coerceAtLeast(1)
            } else {
                12
            }
            round((totalSpend / monthsCount) * 10.0) / 10.0
        }

        val grouped = filtered.groupBy { it.type }
        var largestCategory: ExpenseType? = null
        var largestCategoryAmount = 0.0

        for ((type, list) in grouped) {
            val sum = list.sumOf { it.amount }
            if (sum > largestCategoryAmount) {
                largestCategoryAmount = sum
                largestCategory = type
            }
        }

        val fuelTotal = grouped[ExpenseType.FUEL]?.sumOf { it.amount } ?: 0.0
        val maintenanceTotal = grouped[ExpenseType.MAINTENANCE]?.sumOf { it.amount } ?: 0.0
        val fixedCostsTotal = (
            (grouped[ExpenseType.PARKING]?.sumOf { it.amount } ?: 0.0) +
            (grouped[ExpenseType.TOLL]?.sumOf { it.amount } ?: 0.0) +
            (grouped[ExpenseType.INSURANCE]?.sumOf { it.amount } ?: 0.0) +
            (grouped[ExpenseType.TAX]?.sumOf { it.amount } ?: 0.0)
        )

        return ExpenseReportSummary(
            totalSpend = totalSpend,
            monthlyAverage = monthlyAverage,
            largestCategory = largestCategory,
            largestCategoryAmount = largestCategoryAmount,
            fuelTotal = fuelTotal,
            maintenanceTotal = maintenanceTotal,
            fixedCostsTotal = fixedCostsTotal
        )
    }

    fun getAvailableYears(
        expenses: List<CarExpenseLog>,
        currentYear: Int = DateTimeUtils.today().year
    ): List<Int> {
        val yearsInExpenses = expenses.map { DateTimeUtils.localDateFromMillis(it.date).year }.toSet()
        val combined = (yearsInExpenses + currentYear).sortedDescending()
        return combined
    }

    private fun filterExpenses(
        expenses: List<CarExpenseLog>,
        targetYear: Int,
        targetMonth: Int?
    ): List<CarExpenseLog> {
        return expenses.filter {
            val date = DateTimeUtils.localDateFromMillis(it.date)
            if (date.year != targetYear) return@filter false
            if (targetMonth != null && date.monthNumber != targetMonth) return@filter false
            true
        }
    }
}
