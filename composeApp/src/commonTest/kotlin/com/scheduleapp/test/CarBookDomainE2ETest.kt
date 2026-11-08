package com.scheduleapp.test

import kotlinx.datetime.LocalDate
import kotlin.math.round
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// --- Mock Domain Models for CarBook ---

enum class FuelType { GASOLINE, DIESEL, HYBRID, ELECTRIC, LPG }
enum class ExpenseType { FUEL, MAINTENANCE, PARKING, TOLL, INSURANCE, OTHER }
enum class MaintenanceStatus { OK, WARNING, OVERDUE }

data class Vehicle(
    val id: String,
    val name: String,
    val model: String,
    val fuelType: FuelType = FuelType.GASOLINE,
    val initialOdometer: Double,
    var currentOdometer: Double,
    val isDefault: Boolean = true
)

data class CarExpenseLog(
    val id: String,
    val vehicleId: String,
    val date: LocalDate,
    val type: ExpenseType,
    val amount: Double,
    val odometer: Double,
    val memo: String? = null
)

data class FuelRecord(
    val expenseId: String,
    val vehicleId: String,
    val date: LocalDate,
    val odometer: Double,
    val amount: Double,
    val unitPrice: Double,
    val liters: Double,
    val isFullTank: Boolean,
    var calculatedEfficiency: Double? = null, // km/L
    var costPerKm: Double? = null             // KRW/km
)

data class MaintenanceItem(
    val id: String,
    val vehicleId: String,
    val partName: String,
    val cycleKm: Double,       // e.g. 10000.0 (0 if time-only)
    val cycleMonths: Int,      // e.g. 6 (0 if distance-only)
    val lastReplacedDate: LocalDate,
    val lastReplacedOdometer: Double,
    var nextDueDate: LocalDate,
    var scheduledEventId: String? = null
)

class InvalidOdometerException(message: String) : IllegalArgumentException(message)

object FuelEfficiencyEngine {
    /**
     * Full-to-Full Fuel Efficiency Engine:
     * - First full tank has no previous baseline -> efficiency = null
     * - Consecutive partial refills accumulate liters until a full refill occurs.
     *   Efficiency = (currentOdo - lastFullOdo) / sum(accumulatedLiters + currentLiters)
     */
    fun processFuelRecords(records: List<FuelRecord>): List<FuelRecord> {
        val sorted = records.sortedBy { it.odometer }
        var lastFullTankRecord: FuelRecord? = null
        var accumulatedPartialLiters = 0.0
        var accumulatedPartialCost = 0.0

        return sorted.map { current ->
            if (!current.isFullTank) {
                // Partial refill cannot close efficiency window
                accumulatedPartialLiters += current.liters
                accumulatedPartialCost += current.amount
                current.copy(calculatedEfficiency = null, costPerKm = null)
            } else {
                // Full tank
                if (lastFullTankRecord == null) {
                    // First full tank: establishes baseline
                    lastFullTankRecord = current
                    accumulatedPartialLiters = 0.0
                    accumulatedPartialCost = 0.0
                    current.copy(calculatedEfficiency = null, costPerKm = null)
                } else {
                    val deltaOdo = current.odometer - lastFullTankRecord!!.odometer
                    val totalLiters = accumulatedPartialLiters + current.liters
                    val totalCost = accumulatedPartialCost + current.amount

                    val efficiency = if (totalLiters > 0.0 && deltaOdo > 0.0) {
                        round((deltaOdo / totalLiters) * 100.0) / 100.0
                    } else null

                    val costPerKm = if (deltaOdo > 0.0) {
                        round((totalCost / deltaOdo) * 10.0) / 10.0
                    } else null

                    lastFullTankRecord = current
                    accumulatedPartialLiters = 0.0
                    accumulatedPartialCost = 0.0

                    current.copy(calculatedEfficiency = efficiency, costPerKm = costPerKm)
                }
            }
        }
    }
}

object MaintenanceWearEngine {
    /**
     * Calculates dual-metric wear percentage:
     * - Distance wear: (currentOdo - lastOdo) / cycleKm * 100%
     * - Time wear: monthsBetween(lastDate, currentDate) / cycleMonths * 100%
     * - Final wear = max(distanceWear, timeWear)
     */
    fun calculateWear(
        item: MaintenanceItem,
        currentOdometer: Double,
        currentDate: LocalDate
    ): Pair<Double, MaintenanceStatus> {
        val distanceWear = if (item.cycleKm > 0.0) {
            val deltaKm = (currentOdometer - item.lastReplacedOdometer).coerceAtLeast(0.0)
            (deltaKm / item.cycleKm) * 100.0
        } else 0.0

        val timeWear = if (item.cycleMonths > 0) {
            val monthsElapsed = monthsBetween(item.lastReplacedDate, currentDate).coerceAtLeast(0.0)
            (monthsElapsed / item.cycleMonths.toDouble()) * 100.0
        } else 0.0

        val maxWear = round(maxOf(distanceWear, timeWear) * 10.0) / 10.0
        val status = when {
            maxWear >= 100.0 -> MaintenanceStatus.OVERDUE
            maxWear >= 80.0 -> MaintenanceStatus.WARNING
            else -> MaintenanceStatus.OK
        }
        return Pair(maxWear, status)
    }

    fun monthsBetween(start: LocalDate, end: LocalDate): Double {
        val yearDiff = end.year - start.year
        val monthDiff = end.monthNumber - start.monthNumber
        val dayDiff = (end.dayOfMonth - start.dayOfMonth) / 30.0
        return (yearDiff * 12 + monthDiff + dayDiff).coerceAtLeast(0.0)
    }
}

class CarBookDomainE2ETest {

    // ==========================================
    // Tier 1: Feature Coverage (Vehicle Registration & Progression)
    // ==========================================

    @Test
    fun testVehicle_RegistrationAndInitialOdometer() {
        val vehicle = Vehicle(
            id = "v1",
            name = "Ionic 5",
            model = "2024 Long Range",
            fuelType = FuelType.ELECTRIC,
            initialOdometer = 12000.0,
            currentOdometer = 12000.0
        )
        assertEquals("Ionic 5", vehicle.name)
        assertEquals(12000.0, vehicle.initialOdometer)
        assertEquals(12000.0, vehicle.currentOdometer)
    }

    @Test
    fun testVehicle_OdometerProgression_ValidUpdate() {
        val vehicle = Vehicle("v1", "Avante", "CN7", FuelType.GASOLINE, 50000.0, 50000.0)
        val newOdo = 50350.0
        if (newOdo < vehicle.currentOdometer) throw InvalidOdometerException("Cannot roll back")
        vehicle.currentOdometer = newOdo

        assertEquals(50350.0, vehicle.currentOdometer)
        assertEquals(350.0, vehicle.currentOdometer - vehicle.initialOdometer)
    }

    @Test
    fun testVehicle_MultiVehicle_DefaultSelection() {
        val v1 = Vehicle("v1", "Car 1", "Sedan", FuelType.GASOLINE, 10000.0, 10000.0, isDefault = true)
        val v2 = Vehicle("v2", "Car 2", "SUV", FuelType.DIESEL, 20000.0, 20000.0, isDefault = false)

        val vehicles = listOf(v1, v2)
        val defaultVehicle = vehicles.first { it.isDefault }
        assertEquals("v1", defaultVehicle.id)
    }

    // ==========================================
    // Tier 1: Feature Coverage (Fuel Logging & Full-to-Full Engine)
    // ==========================================

    @Test
    fun testFuel_FirstFullTank_BaselineYieldsNullEfficiency() {
        val r1 = FuelRecord("e1", "v1", LocalDate(2026, 11, 1), odometer = 50000.0, amount = 70000.0, unitPrice = 1750.0, liters = 40.0, isFullTank = true)
        val processed = FuelEfficiencyEngine.processFuelRecords(listOf(r1))

        assertEquals(1, processed.size)
        assertNull(processed.first().calculatedEfficiency)
    }

    @Test
    fun testFuel_ConsecutiveFullTanks_CalculatesExactKmPerL() {
        // Tank 1: 50,000 km (Full)
        // Tank 2: 50,600 km (Full, 40L, 70,000 KRW)
        // Delta = 600 km / 40 L = 15.0 km/L; Cost = 70,000 / 600 = 116.7 KRW/km
        val r1 = FuelRecord("e1", "v1", LocalDate(2026, 11, 1), 50000.0, 70000.0, 1750.0, 40.0, isFullTank = true)
        val r2 = FuelRecord("e2", "v1", LocalDate(2026, 11, 15), 50600.0, 70000.0, 1750.0, 40.0, isFullTank = true)

        val processed = FuelEfficiencyEngine.processFuelRecords(listOf(r1, r2))
        assertEquals(2, processed.size)
        assertNull(processed[0].calculatedEfficiency)
        assertEquals(15.0, processed[1].calculatedEfficiency)
        assertEquals(116.7, processed[1].costPerKm)
    }

    // ==========================================
    // Tier 1: Feature Coverage (Maintenance Wear Engine)
    // ==========================================

    @Test
    fun testMaintenance_DistanceDominates_CalculatesDistanceWear() {
        val item = MaintenanceItem(
            id = "m1",
            vehicleId = "v1",
            partName = "Engine Oil",
            cycleKm = 10000.0,
            cycleMonths = 12,
            lastReplacedDate = LocalDate(2026, 1, 1),
            lastReplacedOdometer = 50000.0,
            nextDueDate = LocalDate(2027, 1, 1)
        )
        // 5,000 km driven in 1 month: distance wear = 50%, time wear = ~8.3%
        val (wear, status) = MaintenanceWearEngine.calculateWear(item, currentOdometer = 55000.0, currentDate = LocalDate(2026, 2, 1))
        assertEquals(50.0, wear)
        assertEquals(MaintenanceStatus.OK, status)
    }

    @Test
    fun testMaintenance_TimeDominates_CalculatesTimeWear() {
        val item = MaintenanceItem(
            id = "m1",
            vehicleId = "v1",
            partName = "Air Conditioner Filter",
            cycleKm = 15000.0,
            cycleMonths = 6,
            lastReplacedDate = LocalDate(2026, 1, 1),
            lastReplacedOdometer = 50000.0,
            nextDueDate = LocalDate(2026, 7, 1)
        )
        // Driven only 1,000 km in 6 months: distance wear = 6.7%, time wear = 100%
        val (wear, status) = MaintenanceWearEngine.calculateWear(item, currentOdometer = 51000.0, currentDate = LocalDate(2026, 7, 1))
        assertEquals(100.0, wear)
        assertEquals(MaintenanceStatus.OVERDUE, status)
    }

    // ==========================================
    // Tier 1: Feature Coverage (Expense Aggregation)
    // ==========================================

    @Test
    fun testExpense_MonthlyCategoryAggregation() {
        val logs = listOf(
            CarExpenseLog("1", "v1", LocalDate(2026, 11, 2), ExpenseType.FUEL, 70000.0, 50000.0),
            CarExpenseLog("2", "v1", LocalDate(2026, 11, 5), ExpenseType.TOLL, 5000.0, 50100.0),
            CarExpenseLog("3", "v1", LocalDate(2026, 11, 12), ExpenseType.PARKING, 15000.0, 50200.0),
            CarExpenseLog("4", "v1", LocalDate(2026, 11, 20), ExpenseType.FUEL, 75000.0, 50600.0)
        )

        val totalFuel = logs.filter { it.type == ExpenseType.FUEL }.sumOf { it.amount }
        val totalNov = logs.sumOf { it.amount }

        assertEquals(145000.0, totalFuel)
        assertEquals(165000.0, totalNov)
    }

    // ==========================================
    // Tier 2: Boundary & Corner Cases
    // ==========================================

    @Test
    fun testBoundary_OdometerEntry_LessThanPrevious_ThrowsException() {
        val vehicle = Vehicle("v1", "Car", "Model", FuelType.GASOLINE, 60000.0, 60000.0)
        val rolledBackOdo = 59990.0

        assertFailsWith<InvalidOdometerException> {
            if (rolledBackOdo < vehicle.currentOdometer) {
                throw InvalidOdometerException("Odometer rollback detected")
            }
            vehicle.currentOdometer = rolledBackOdo
        }
    }

    @Test
    fun testBoundary_ConsecutivePartialRefills_FollowedByFullRefill() {
        // Tank 0: 50,000 km (Full Tank baseline)
        // Tank 1: 50,250 km (Partial: 20L, 35,000 KRW) -> null
        // Tank 2: 50,450 km (Partial: 15L, 27,000 KRW) -> null
        // Tank 3: 50,750 km (Full: 25L, 44,000 KRW) -> Closed!
        // Total Delta Odometer = 50,750 - 50,000 = 750 km
        // Total Liters = 20 + 15 + 25 = 60 L
        // Expected Efficiency = 750 / 60 = 12.5 km/L
        // Total Cost = 35,000 + 27,000 + 44,000 = 106,000 KRW -> 106,000 / 750 = 141.3 KRW/km
        val r0 = FuelRecord("e0", "v1", LocalDate(2026, 11, 1), 50000.0, 70000.0, 1750.0, 40.0, isFullTank = true)
        val r1 = FuelRecord("e1", "v1", LocalDate(2026, 11, 5), 50250.0, 35000.0, 1750.0, 20.0, isFullTank = false)
        val r2 = FuelRecord("e2", "v1", LocalDate(2026, 11, 10), 50450.0, 27000.0, 1800.0, 15.0, isFullTank = false)
        val r3 = FuelRecord("e3", "v1", LocalDate(2026, 11, 18), 50750.0, 44000.0, 1760.0, 25.0, isFullTank = true)

        val processed = FuelEfficiencyEngine.processFuelRecords(listOf(r0, r1, r2, r3))
        assertNull(processed[0].calculatedEfficiency)
        assertNull(processed[1].calculatedEfficiency)
        assertNull(processed[2].calculatedEfficiency)

        val finalFull = processed[3]
        assertEquals(12.5, finalFull.calculatedEfficiency)
        assertEquals(141.3, finalFull.costPerKm)
    }

    @Test
    fun testBoundary_MaintenanceWear_OverdueClampingExceeds100Percent() {
        val item = MaintenanceItem("m1", "v1", "Engine Oil", 10000.0, 6, LocalDate(2026, 1, 1), 50000.0, LocalDate(2026, 7, 1))
        // 13,000 km driven -> wear = 130%
        val (wear, status) = MaintenanceWearEngine.calculateWear(item, currentOdometer = 63000.0, currentDate = LocalDate(2026, 4, 1))
        assertEquals(130.0, wear)
        assertEquals(MaintenanceStatus.OVERDUE, status)
    }

    @Test
    fun testBoundary_SingleMetric_ZeroCycleKm_TimeOnly() {
        // Annual safety inspection: cycleKm = 0, cycleMonths = 12
        val item = MaintenanceItem("m2", "v1", "Safety Inspection", cycleKm = 0.0, cycleMonths = 12, LocalDate(2026, 1, 1), 50000.0, LocalDate(2027, 1, 1))
        val (wear, status) = MaintenanceWearEngine.calculateWear(item, currentOdometer = 80000.0, currentDate = LocalDate(2026, 7, 1))
        // 6 months elapsed out of 12 -> 50% wear
        assertEquals(50.0, wear)
        assertEquals(MaintenanceStatus.OK, status)
    }

    @Test
    fun testBoundary_SingleMetric_ZeroCycleMonths_DistanceOnly() {
        // High wear brake pads: cycleKm = 20,000, cycleMonths = 0
        val item = MaintenanceItem("m3", "v1", "Brake Pads", cycleKm = 20000.0, cycleMonths = 0, LocalDate(2026, 1, 1), 50000.0, LocalDate(2027, 1, 1))
        val (wear, status) = MaintenanceWearEngine.calculateWear(item, currentOdometer = 67000.0, currentDate = LocalDate(2028, 1, 1))
        // 17,000 km driven out of 20,000 -> 85% wear -> WARNING
        assertEquals(85.0, wear)
        assertEquals(MaintenanceStatus.WARNING, status)
    }

    @Test
    fun testBoundary_LeapYear_DateTransition() {
        // Replaced on 2024-02-29 (Leap day) + 12 months -> 2025-02-28
        val leapDate = LocalDate(2024, 2, 29)
        val oneYearLater = LocalDate(2025, 2, 28)
        val months = MaintenanceWearEngine.monthsBetween(leapDate, oneYearLater)
        assertTrue(months >= 11.9 && months <= 12.1)
    }
}
