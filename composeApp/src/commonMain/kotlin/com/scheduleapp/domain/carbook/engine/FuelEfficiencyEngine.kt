package com.scheduleapp.domain.carbook.engine

import com.scheduleapp.domain.carbook.model.FuelRecord
import kotlin.math.round

object FuelEfficiencyEngine {

    /**
     * Validates that new odometer is greater than or equal to previous odometer.
     * Throws [IllegalArgumentException] if rollback is detected.
     */
    fun validateOdometer(previousOdometer: Double, newOdometer: Double) {
        if (newOdometer < previousOdometer) {
            throw IllegalArgumentException(
                "Odometer rollback detected: new odometer ($newOdometer km) cannot be less than previous ($previousOdometer km)"
            )
        }
    }

    /**
     * Full-to-Full Fuel Efficiency Engine:
     * - First full tank has no previous baseline -> efficiency = null
     * - Consecutive partial refills accumulate liters and costs until a full refill occurs.
     *   Efficiency (km/L) = Delta Odometer / sum(accumulatedPartialLiters + currentLiters)
     *   Cost per km (KRW/km) = sum(accumulatedPartialCost + currentAmount) / Delta Odometer
     *
     * @param records List of fuel records
     * @param enforceAntiRollback If true, checks chronological monotonicity and throws IllegalArgumentException if rollback exists.
     */
    fun processFuelRecords(
        records: List<FuelRecord>,
        enforceAntiRollback: Boolean = false
    ): List<FuelRecord> {
        if (records.isEmpty()) return emptyList()

        if (enforceAntiRollback) {
            val sortedByDate = records.sortedBy { it.date }
            for (i in 1 until sortedByDate.size) {
                if (sortedByDate[i].odometer < sortedByDate[i - 1].odometer) {
                    throw IllegalArgumentException(
                        "Odometer rollback detected: record at ${sortedByDate[i].odometer} km is less than prior ${sortedByDate[i - 1].odometer} km"
                    )
                }
            }
        }

        val sorted = records.sortedBy { it.odometer }
        var lastFullTankRecord: FuelRecord? = null
        var accumulatedPartialLiters = 0.0
        var accumulatedPartialCost = 0.0

        return sorted.map { current ->
            if (!current.isFullTank) {
                accumulatedPartialLiters += current.liters
                accumulatedPartialCost += current.amount
                current.copy(calculatedEfficiency = null, costPerKm = null)
            } else {
                if (lastFullTankRecord == null) {
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

    /**
     * Calculates cumulative average efficiency (km/L) across all closed full-to-full refill windows.
     */
    fun calculateCumulativeAverageEfficiency(records: List<FuelRecord>): Double? {
        if (records.isEmpty()) return null

        var totalClosedDeltaOdo = 0.0
        var totalClosedLiters = 0.0
        var lastFullOdo: Double? = null
        var accumulatedLiters = 0.0

        val sorted = records.sortedBy { it.odometer }
        for (rec in sorted) {
            if (!rec.isFullTank) {
                accumulatedLiters += rec.liters
            } else {
                if (lastFullOdo == null) {
                    lastFullOdo = rec.odometer
                    accumulatedLiters = 0.0
                } else {
                    val deltaOdo = rec.odometer - lastFullOdo
                    val liters = accumulatedLiters + rec.liters
                    if (deltaOdo > 0.0 && liters > 0.0) {
                        totalClosedDeltaOdo += deltaOdo
                        totalClosedLiters += liters
                    }
                    lastFullOdo = rec.odometer
                    accumulatedLiters = 0.0
                }
            }
        }

        return if (totalClosedLiters > 0.0 && totalClosedDeltaOdo > 0.0) {
            round((totalClosedDeltaOdo / totalClosedLiters) * 100.0) / 100.0
        } else null
    }

    /**
     * Calculates cumulative average cost per km (KRW/km) across all closed full-to-full refill windows.
     */
    fun calculateCumulativeAverageCostPerKm(records: List<FuelRecord>): Double? {
        if (records.isEmpty()) return null

        var totalClosedDeltaOdo = 0.0
        var totalClosedCost = 0.0
        var lastFullOdo: Double? = null
        var accumulatedCost = 0.0

        val sorted = records.sortedBy { it.odometer }
        for (rec in sorted) {
            if (!rec.isFullTank) {
                accumulatedCost += rec.amount
            } else {
                if (lastFullOdo == null) {
                    lastFullOdo = rec.odometer
                    accumulatedCost = 0.0
                } else {
                    val deltaOdo = rec.odometer - lastFullOdo
                    val cost = accumulatedCost + rec.amount
                    if (deltaOdo > 0.0) {
                        totalClosedDeltaOdo += deltaOdo
                        totalClosedCost += cost
                    }
                    lastFullOdo = rec.odometer
                    accumulatedCost = 0.0
                }
            }
        }

        return if (totalClosedDeltaOdo > 0.0) {
            round((totalClosedCost / totalClosedDeltaOdo) * 10.0) / 10.0
        } else null
    }

    /**
     * Aggregated statistics summary for fuel operations.
     */
    fun calculateStatistics(records: List<FuelRecord>): FuelStatistics {
        val avgEfficiency = calculateCumulativeAverageEfficiency(records)
        val avgCost = calculateCumulativeAverageCostPerKm(records)
        val totalCost = records.sumOf { it.amount }
        val totalLiters = records.sumOf { it.liters }
        val minOdo = records.minOfOrNull { it.odometer } ?: 0.0
        val maxOdo = records.maxOfOrNull { it.odometer } ?: 0.0
        val totalDistance = (maxOdo - minOdo).coerceAtLeast(0.0)

        return FuelStatistics(
            averageKmPerL = avgEfficiency,
            averageCostPerKm = avgCost,
            totalFuelCost = totalCost,
            totalLiters = totalLiters,
            totalDrivenDistanceKm = totalDistance
        )
    }
}

data class FuelStatistics(
    val averageKmPerL: Double?,
    val averageCostPerKm: Double?,
    val totalFuelCost: Double,
    val totalLiters: Double,
    val totalDrivenDistanceKm: Double
)
