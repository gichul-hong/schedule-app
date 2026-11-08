package com.scheduleapp.domain.carbook

import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.carbook.engine.ExpenseReportEngine
import com.scheduleapp.domain.carbook.model.CarExpenseLog
import com.scheduleapp.domain.carbook.model.ExpenseType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExpenseReportEngineTest {

    private fun dateMillis(year: Int, month: Int, day: Int): Long {
        return DateTimeUtils.startOfDayMillis(LocalDate(year, month, day))
    }

    @Test
    fun testGenerateMonthlyTrend_Generates12MonthsCorrectly() {
        val expenses = listOf(
            CarExpenseLog(1L, 1L, dateMillis(2026, 1, 15), ExpenseType.FUEL, 70000.0, 10500.0),
            CarExpenseLog(2L, 1L, dateMillis(2026, 1, 20), ExpenseType.PARKING, 10000.0, 10600.0),
            CarExpenseLog(3L, 1L, dateMillis(2026, 2, 10), ExpenseType.MAINTENANCE, 150000.0, 11000.0),
            CarExpenseLog(4L, 1L, dateMillis(2026, 5, 5), ExpenseType.TOLL, 15000.0, 13000.0)
        )

        val trend = ExpenseReportEngine.generateMonthlyTrend(expenses, 2026)
        assertEquals(12, trend.size)

        // January
        val jan = trend[0]
        assertEquals(1, jan.month)
        assertEquals("1월", jan.label)
        assertEquals(80000.0, jan.totalAmount)
        assertEquals(70000.0, jan.fuelAmount)
        assertEquals(0.0, jan.maintenanceAmount)
        assertEquals(10000.0, jan.otherAmount)

        // February
        val feb = trend[1]
        assertEquals(2, feb.month)
        assertEquals(150000.0, feb.totalAmount)
        assertEquals(150000.0, feb.maintenanceAmount)

        // March (no expenses)
        val mar = trend[2]
        assertEquals(0.0, mar.totalAmount)
    }

    @Test
    fun testGenerateCategoryBreakdown_CalculatesPercentagesAndColors() {
        val expenses = listOf(
            CarExpenseLog(1L, 1L, dateMillis(2026, 3, 1), ExpenseType.FUEL, 500000.0, 10000.0),        // 50%
            CarExpenseLog(2L, 1L, dateMillis(2026, 3, 2), ExpenseType.MAINTENANCE, 300000.0, 10000.0), // 30%
            CarExpenseLog(3L, 1L, dateMillis(2026, 3, 3), ExpenseType.PARKING, 100000.0, 10000.0),     // 10%
            CarExpenseLog(4L, 1L, dateMillis(2026, 3, 4), ExpenseType.TOLL, 100000.0, 10000.0)         // 10%
        )

        val breakdown = ExpenseReportEngine.generateCategoryBreakdown(expenses, 2026, 3)
        assertEquals(4, breakdown.size)

        // Slices should be sorted descending by amount
        assertEquals(ExpenseType.FUEL, breakdown[0].type)
        assertEquals(50.0, breakdown[0].percentage)
        assertEquals(0xFFFFA000L, breakdown[0].colorHex)

        assertEquals(ExpenseType.MAINTENANCE, breakdown[1].type)
        assertEquals(30.0, breakdown[1].percentage)

        assertEquals(ExpenseType.PARKING, breakdown[2].type)
        assertEquals(10.0, breakdown[2].percentage)

        assertEquals(ExpenseType.TOLL, breakdown[3].type)
        assertEquals(10.0, breakdown[3].percentage)
    }

    @Test
    fun testGenerateSummary_ComputesAggregatesAndLargestCategory() {
        val expenses = listOf(
            CarExpenseLog(1L, 1L, dateMillis(2026, 1, 10), ExpenseType.FUEL, 120000.0, 10000.0),
            CarExpenseLog(2L, 1L, dateMillis(2026, 2, 10), ExpenseType.FUEL, 130000.0, 11000.0),
            CarExpenseLog(3L, 1L, dateMillis(2026, 4, 15), ExpenseType.MAINTENANCE, 250000.0, 13000.0),
            CarExpenseLog(4L, 1L, dateMillis(2026, 6, 20), ExpenseType.PARKING, 50000.0, 15000.0),
            CarExpenseLog(5L, 1L, dateMillis(2026, 7, 25), ExpenseType.INSURANCE, 650000.0, 16000.0)
        )

        val summary = ExpenseReportEngine.generateSummary(expenses, 2026)

        val expectedTotal = 120000.0 + 130000.0 + 250000.0 + 50000.0 + 650000.0
        assertEquals(expectedTotal, summary.totalSpend)
        assertEquals(250000.0, summary.fuelTotal)
        assertEquals(250000.0, summary.maintenanceTotal)
        assertEquals(700000.0, summary.fixedCostsTotal) // parking(50k) + insurance(650k)

        assertEquals(ExpenseType.INSURANCE, summary.largestCategory)
        assertEquals(650000.0, summary.largestCategoryAmount)
        assertTrue(summary.monthlyAverage > 0.0)
    }

    @Test
    fun testGetAvailableYears_ReturnsYearsDescending() {
        val expenses = listOf(
            CarExpenseLog(1L, 1L, dateMillis(2024, 1, 1), ExpenseType.FUEL, 50000.0, 1000.0),
            CarExpenseLog(2L, 1L, dateMillis(2025, 6, 1), ExpenseType.FUEL, 60000.0, 2000.0),
            CarExpenseLog(3L, 1L, dateMillis(2026, 3, 1), ExpenseType.FUEL, 70000.0, 3000.0)
        )

        val years = ExpenseReportEngine.getAvailableYears(expenses, 2026)
        assertEquals(listOf(2026, 2025, 2024), years)
    }
}
