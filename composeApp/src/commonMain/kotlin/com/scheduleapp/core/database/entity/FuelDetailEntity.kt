package com.scheduleapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "fuel_details",
    foreignKeys = [
        ForeignKey(
            entity = CarExpenseLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FuelDetailEntity(
    @PrimaryKey
    val expenseId: Long,
    val unitPrice: Double,
    val liters: Double,
    val isFullTank: Boolean = true,
    val calculatedEfficiency: Double? = null
)
