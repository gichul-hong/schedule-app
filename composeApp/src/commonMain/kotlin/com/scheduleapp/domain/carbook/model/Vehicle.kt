package com.scheduleapp.domain.carbook.model

data class Vehicle(
    val id: Long = 0,
    val name: String,
    val model: String,
    val fuelType: FuelType = FuelType.GASOLINE,
    val initialOdometer: Double = 0.0,
    val currentOdometer: Double = 0.0,
    val isDefault: Boolean = false
)
