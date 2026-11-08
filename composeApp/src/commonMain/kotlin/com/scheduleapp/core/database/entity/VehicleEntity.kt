package com.scheduleapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val model: String,
    val fuelType: String = "GASOLINE",
    val initialOdometer: Double = 0.0,
    val currentOdometer: Double = 0.0,
    val isDefault: Boolean = false
)
