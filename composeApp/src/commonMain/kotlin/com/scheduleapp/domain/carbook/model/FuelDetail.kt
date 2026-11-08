package com.scheduleapp.domain.carbook.model

data class FuelDetail(
    val expenseId: Long,
    val unitPrice: Double,
    val liters: Double,
    val isFullTank: Boolean = true,
    val calculatedEfficiency: Double? = null
)
