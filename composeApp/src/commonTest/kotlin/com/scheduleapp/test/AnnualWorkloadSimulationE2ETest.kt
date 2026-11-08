package com.scheduleapp.test

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AnnualWorkloadSimulationE2ETest {

    @Test
    fun testTier4_OneYearOwnershipLifecycleSimulation() {
        // --- 1. Day 1 (Jan 1, 2026): Vehicle Registration & Routine Setup ---
        val vehicle = Vehicle(
            id = "v_annual",
            name = "Family Tucson",
            model = "1.6T Hybrid",
            fuelType = FuelType.HYBRID,
            initialOdometer = 10000.0,
            currentOdometer = 10000.0
        )

        var engineOil = MaintenanceItem(
            id = "m_oil",
            vehicleId = vehicle.id,
            partName = "Engine Oil",
            cycleKm = 10000.0,
            cycleMonths = 6,
            lastReplacedDate = LocalDate(2026, 1, 1),
            lastReplacedOdometer = 10000.0,
            nextDueDate = LocalDate(2026, 7, 1)
        )

        val commuteRoutine = Routine(
            id = "r_commute",
            title = "Commute Log",
            repeatDaysMask = 31, // Mon-Fri
            startDate = LocalDate(2026, 1, 1)
        )

        val expenseLedger = mutableListOf<CarExpenseLog>()
        val fuelRecords = mutableListOf<FuelRecord>()
        val routineLogs = mutableListOf<RoutineLog>()

        // Initial Full Tank baseline on Day 1
        val baselineFuel = FuelRecord("f0", vehicle.id, LocalDate(2026, 1, 1), 10000.0, 72000.0, 1800.0, 40.0, isFullTank = true)
        fuelRecords.add(baselineFuel)
        expenseLedger.add(CarExpenseLog("exp_f0", vehicle.id, baselineFuel.date, ExpenseType.FUEL, baselineFuel.amount, baselineFuel.odometer))

        // --- 2. 365-Day Simulation Loop (Jan 1, 2026 to Dec 31, 2026) ---
        var currentOdo = 10000.0
        var totalScheduledWorkdays = 0
        var completedRoutines = 0

        for (dayOfYear in 1..365) {
            val epochDay = LocalDate(2026, 1, 1).toEpochDays() + (dayOfYear - 1)
            val currentDate = LocalDate.fromEpochDays(epochDay)
            val dayOfWeek = currentDate.dayOfWeek.isoDayNumber // 1=Mon .. 7=Sun

            // (A) Routine: If Mon-Fri, log completion
            if (RoutineStreakEngine.isScheduledOn(commuteRoutine.repeatDaysMask, dayOfWeek)) {
                totalScheduledWorkdays++
                // Simulate 95% habit adherence (skip occasionally)
                val doHabit = (dayOfYear % 25 != 0)
                if (doHabit) {
                    routineLogs.add(RoutineLog("rl_$dayOfYear", commuteRoutine.id, currentDate, isCompleted = true))
                    completedRoutines++
                }
                // Daily commute adds ~55 km on weekdays
                currentOdo += 55.0
            } else {
                // Weekend errands: ~30 km
                currentOdo += 30.0
            }

            vehicle.currentOdometer = currentOdo

            // (B) Bi-weekly Fueling (Every 14 days)
            if (dayOfYear % 14 == 0) {
                val isFull = (dayOfYear % 28 == 0) // Alternates: Partial, then Full
                val liters = if (isFull) 42.0 else 25.0
                val amount = liters * 1800.0
                val fuelRec = FuelRecord("f_$dayOfYear", vehicle.id, currentDate, currentOdo, amount, 1800.0, liters, isFullTank = isFull)
                fuelRecords.add(fuelRec)
                expenseLedger.add(CarExpenseLog("exp_f_$dayOfYear", vehicle.id, currentDate, ExpenseType.FUEL, amount, currentOdo))
            }

            // (C) Monthly Fixed Expenses (Day 25 of every month)
            if (currentDate.dayOfMonth == 25) {
                // Parking: 100,000 KRW
                expenseLedger.add(CarExpenseLog("exp_p_$dayOfYear", vehicle.id, currentDate, ExpenseType.PARKING, 100000.0, currentOdo))
                // Tolls: 35,000 KRW
                expenseLedger.add(CarExpenseLog("exp_t_$dayOfYear", vehicle.id, currentDate, ExpenseType.TOLL, 35000.0, currentOdo))
            }

            // (D) Day 180 (End of June): Engine Oil Service Trigger & Execution
            if (dayOfYear == 180) {
                val (wear, _) = MaintenanceWearEngine.calculateWear(engineOil, vehicle.currentOdometer, currentDate)
                assertTrue(wear >= 90.0, "Engine oil should be due near 180 days (~10,000 km driven)")

                // Perform service
                val serviceCost = 135000.0
                expenseLedger.add(CarExpenseLog("exp_maint_1", vehicle.id, currentDate, ExpenseType.MAINTENANCE, serviceCost, currentOdo, "Engine Oil & Filter"))
                engineOil = engineOil.copy(
                    lastReplacedDate = currentDate,
                    lastReplacedOdometer = currentOdo,
                    nextDueDate = LocalDate(2026, 12, 28)
                )
            }

            // (E) Annual Insurance in September (Day 260)
            if (dayOfYear == 260) {
                expenseLedger.add(CarExpenseLog("exp_ins", vehicle.id, currentDate, ExpenseType.INSURANCE, 880000.0, currentOdo, "Annual Auto Insurance"))
            }
        }

        // --- 3. End-of-Year System Assertions ---

        // (A) Total Odometer Progression
        assertTrue(vehicle.currentOdometer > 25000.0, "Odometer should exceed 25,000 km over 1 year of daily commuting")
        val annualMileage = vehicle.currentOdometer - vehicle.initialOdometer
        assertTrue(annualMileage in 15000.0..20000.0, "Annual mileage should be around 17,000 km")

        // (B) Fuel Engine Verification
        val processedFuel = FuelEfficiencyEngine.processFuelRecords(fuelRecords)
        val validEfficiencies = processedFuel.mapNotNull { it.calculatedEfficiency }
        assertTrue(validEfficiencies.isNotEmpty(), "Should calculate fuel efficiency on all full refills")
        val avgEfficiency = validEfficiencies.average()
        // Tucson Hybrid fuel efficiency typically 14 ~ 18 km/L
        assertTrue(avgEfficiency in 12.0..20.0, "Average fuel efficiency should be realistic: $avgEfficiency km/L")

        // (C) Annual Expense Aggregation & Ratio Assertions
        val totalSpent = expenseLedger.sumOf { it.amount }
        val fuelSpent = expenseLedger.filter { it.type == ExpenseType.FUEL }.sumOf { it.amount }
        val maintenanceSpent = expenseLedger.filter { it.type == ExpenseType.MAINTENANCE }.sumOf { it.amount }
        val parkingSpent = expenseLedger.filter { it.type == ExpenseType.PARKING }.sumOf { it.amount }
        val tollSpent = expenseLedger.filter { it.type == ExpenseType.TOLL }.sumOf { it.amount }
        val insuranceSpent = expenseLedger.filter { it.type == ExpenseType.INSURANCE }.sumOf { it.amount }

        assertEquals(135000.0, maintenanceSpent)
        assertEquals(880000.0, insuranceSpent)
        assertEquals(1200000.0, parkingSpent) // 100k * 12 months
        assertEquals(420000.0, tollSpent)    // 35k * 12 months
        assertTrue(fuelSpent > 1500000.0, "Annual fuel expense should reflect mileage")
        assertTrue(totalSpent > 4000000.0, "Total annual ownership cost should be calculated accurately")

        // (D) Routine Habit Tracking & Adherence
        assertTrue(totalScheduledWorkdays in 260..262)
        val adherenceRatio = completedRoutines.toDouble() / totalScheduledWorkdays.toDouble()
        assertTrue(adherenceRatio > 0.90, "Adherence should exceed 90%")
    }
}
