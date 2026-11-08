package com.scheduleapp.domain.carbook.model

data class CarExpenseLog(
    val id: Long = 0,
    val vehicleId: Long,
    val date: Long,
    val type: ExpenseType,
    val amount: Double,
    val odometer: Double,
    val memo: String? = null
)
