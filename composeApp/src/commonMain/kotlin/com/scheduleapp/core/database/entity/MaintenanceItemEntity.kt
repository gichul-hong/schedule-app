package com.scheduleapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "maintenance_items",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId")]
)
data class MaintenanceItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val partName: String,
    val cycleKm: Double,
    val cycleMonths: Int,
    val lastReplacedDate: Long? = null,
    val lastReplacedOdometer: Double? = null,
    val nextDueDate: Long? = null,
    val scheduledEventId: Long? = null
)
