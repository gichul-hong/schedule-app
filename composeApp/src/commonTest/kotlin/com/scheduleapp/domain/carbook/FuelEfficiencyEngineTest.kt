package com.scheduleapp.domain.carbook

import com.scheduleapp.domain.carbook.engine.FuelEfficiencyEngine
import com.scheduleapp.domain.carbook.model.FuelRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FuelEfficiencyEngineTest {

    // ==========================================
    // 1. Baseline Full Tank & Consecutive Full Tanks
    // ==========================================

    @Test
    fun testFirstFullRefill_EstablishesBaseline_ReturnsNullEfficiency() {
        val r1 = FuelRecord(
            expenseId = 1L,
            vehicleId = 1L,
            date = 1000L,
            odometer = 50000.0,
            amount = 70000.0,
            unitPrice = 1750.0,
            liters = 40.0,
            isFullTank = true
        )

        val processed = FuelEfficiencyEngine.processFuelRecords(listOf(r1))
        assertEquals(1, processed.size)
        assertNull(processed[0].calculatedEfficiency, "First full refill baseline should have null efficiency")
        assertNull(processed[0].costPerKm, "First full refill baseline should have null cost per km")
    }

    @Test
    fun testConsecutiveFullRefills_CalculatesExactKmPerLAndCostPerKm() {
        // Tank 1: 50,000 km (Full baseline)
        // Tank 2: 50,600 km (Full, 40L, 70,000 KRW)
        // Delta = 600 km / 40 L = 15.0 km/L; Cost = 70,000 / 600 = 116.7 KRW/km
        val r1 = FuelRecord(1L, 1L, 1000L, 50000.0, 70000.0, 1750.0, 40.0, isFullTank = true)
        val r2 = FuelRecord(2L, 1L, 2000L, 50600.0, 70000.0, 1750.0, 40.0, isFullTank = true)

        val processed = FuelEfficiencyEngine.processFuelRecords(listOf(r1, r2))
        assertEquals(2, processed.size)

        assertNull(processed[0].calculatedEfficiency)
        assertEquals(15.0, processed[1].calculatedEfficiency)
        assertEquals(116.7, processed[1].costPerKm)
    }

    // ==========================================
    // 2. Partial Refills & Window Accumulation
    // ==========================================

    @Test
    fun testPartialRefillAlone_ReturnsNullEfficiency() {
        val r1 = FuelRecord(1L, 1L, 1000L, 50000.0, 35000.0, 1750.0, 20.0, isFullTank = false)

        val processed = FuelEfficiencyEngine.processFuelRecords(listOf(r1))
        assertEquals(1, processed.size)
        assertNull(processed[0].calculatedEfficiency, "Partial refill cannot compute fuel economy")
    }

    @Test
    fun testConsecutivePartialRefills_FollowedByFullRefill_AccumulatesLitersAndCosts() {
        // Tank 0: 50,000 km (Full Tank baseline)
        // Tank 1: 50,250 km (Partial: 20L, 35,000 KRW) -> null
        // Tank 2: 50,450 km (Partial: 15L, 27,000 KRW) -> null
        // Tank 3: 50,750 km (Full: 25L, 44,000 KRW) -> Closes window!
        // Total Delta = 50,750 - 50,000 = 750 km
        // Total Liters = 20 + 15 + 25 = 60 L
        // Expected Efficiency = 750 / 60 = 12.5 km/L
        // Total Cost = 35,000 + 27,000 + 44,000 = 106,000 KRW -> 106,000 / 750 = 141.3 KRW/km
        val r0 = FuelRecord(1L, 1L, 1000L, 50000.0, 70000.0, 1750.0, 40.0, isFullTank = true)
        val r1 = FuelRecord(2L, 1L, 2000L, 50250.0, 35000.0, 1750.0, 20.0, isFullTank = false)
        val r2 = FuelRecord(3L, 1L, 3000L, 50450.0, 27000.0, 1800.0, 15.0, isFullTank = false)
        val r3 = FuelRecord(4L, 1L, 4000L, 50750.0, 44000.0, 1760.0, 25.0, isFullTank = true)

        val processed = FuelEfficiencyEngine.processFuelRecords(listOf(r0, r1, r2, r3))
        assertEquals(4, processed.size)

        assertNull(processed[0].calculatedEfficiency)
        assertNull(processed[1].calculatedEfficiency)
        assertNull(processed[2].calculatedEfficiency)

        val closed = processed[3]
        assertEquals(12.5, closed.calculatedEfficiency)
        assertEquals(141.3, closed.costPerKm)
    }

    // ==========================================
    // 3. Cumulative Statistics Across All Closed Windows
    // ==========================================

    @Test
    fun testCumulativeAverageEfficiency_AcrossMultipleWindows() {
        // Window 1: 50,000 -> 50,600 (600 km, 40L, 70,000 KRW) -> 15.0 km/L
        // Window 2: 50,600 -> 51,350 (750 km, 60L (20+15+25), 106,000 KRW) -> 12.5 km/L
        // Cumulative Delta = 600 + 750 = 1,350 km
        // Cumulative Liters = 40 + 60 = 100 L
        // Cumulative Efficiency = 1,350 / 100 = 13.5 km/L
        // Cumulative Cost = 70,000 + 106,000 = 176,000 KRW
        // Cumulative Cost per km = 176,000 / 1,350 = 130.4 KRW/km
        val r0 = FuelRecord(1L, 1L, 1000L, 50000.0, 70000.0, 1750.0, 40.0, isFullTank = true)
        val r1 = FuelRecord(2L, 1L, 2000L, 50600.0, 70000.0, 1750.0, 40.0, isFullTank = true)
        val r2 = FuelRecord(3L, 1L, 3000L, 50850.0, 35000.0, 1750.0, 20.0, isFullTank = false)
        val r3 = FuelRecord(4L, 1L, 4000L, 51050.0, 27000.0, 1800.0, 15.0, isFullTank = false)
        val r4 = FuelRecord(5L, 1L, 5000L, 51350.0, 44000.0, 1760.0, 25.0, isFullTank = true)

        val records = listOf(r0, r1, r2, r3, r4)
        val avgEfficiency = FuelEfficiencyEngine.calculateCumulativeAverageEfficiency(records)
        val avgCost = FuelEfficiencyEngine.calculateCumulativeAverageCostPerKm(records)

        assertEquals(13.5, avgEfficiency)
        assertEquals(130.4, avgCost)

        val stats = FuelEfficiencyEngine.calculateStatistics(records)
        assertEquals(13.5, stats.averageKmPerL)
        assertEquals(130.4, stats.averageCostPerKm)
        assertEquals(246000.0, stats.totalFuelCost)
        assertEquals(140.0, stats.totalLiters)
        assertEquals(1350.0, stats.totalDrivenDistanceKm)
    }

    // ==========================================
    // 4. Anti-Rollback Odometer Enforcement
    // ==========================================

    @Test
    fun testAntiRollback_ThrowsIllegalArgumentException_WhenNewOdometerIsLower() {
        assertFailsWith<IllegalArgumentException> {
            FuelEfficiencyEngine.validateOdometer(previousOdometer = 50000.0, newOdometer = 49990.0)
        }
    }

    @Test
    fun testAntiRollback_AcceptsSameOrHigherOdometer() {
        // Equal odometer is valid (e.g. multiple transactions at same stop)
        FuelEfficiencyEngine.validateOdometer(previousOdometer = 50000.0, newOdometer = 50000.0)
        // Higher odometer is valid
        FuelEfficiencyEngine.validateOdometer(previousOdometer = 50000.0, newOdometer = 50010.0)
    }

    @Test
    fun testProcessFuelRecords_WithAntiRollbackEnforcement_ThrowsOnChronologicalDecrease() {
        val r1 = FuelRecord(1L, 1L, 1000L, 50000.0, 70000.0, 1750.0, 40.0, isFullTank = true)
        val r2 = FuelRecord(2L, 1L, 2000L, 49500.0, 70000.0, 1750.0, 40.0, isFullTank = true)

        assertFailsWith<IllegalArgumentException> {
            FuelEfficiencyEngine.processFuelRecords(listOf(r1, r2), enforceAntiRollback = true)
        }
    }

    @Test
    fun testEmptyRecords_ReturnsGracefully() {
        val emptyProcessed = FuelEfficiencyEngine.processFuelRecords(emptyList())
        assertTrue(emptyProcessed.isEmpty())
        assertNull(FuelEfficiencyEngine.calculateCumulativeAverageEfficiency(emptyList()))
        assertNull(FuelEfficiencyEngine.calculateCumulativeAverageCostPerKm(emptyList()))
    }
}
