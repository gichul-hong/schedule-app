package com.scheduleapp.domain.carbook.model

data class FuelRecord(
    val expenseId: Long = 0,
    val vehicleId: Long,
    val date: Long,
    val odometer: Double,
    val amount: Double,
    val unitPrice: Double,
    val liters: Double,
    val isFullTank: Boolean = true,
    val calculatedEfficiency: Double? = null, // km/L
    val costPerKm: Double? = null,             // KRW/km
    val memo: String? = null
)
