package com.scheduleapp.domain.carbook.model

data class MaintenanceItem(
    val id: Long = 0,
    val vehicleId: Long,
    val partName: String,
    val cycleKm: Double,       // e.g. 10000.0 (0 if time-only)
    val cycleMonths: Int,      // e.g. 12 (0 if distance-only)
    val lastReplacedDate: Long, // epoch milliseconds
    val lastReplacedOdometer: Double,
    val nextDueDate: Long,      // epoch milliseconds
    val scheduledEventId: Long? = null
)

data class MaintenancePreset(
    val partName: String,
    val cycleKm: Double,
    val cycleMonths: Int,
    val description: String
) {
    companion object {
        val DEFAULT_PRESETS = listOf(
            MaintenancePreset("엔진오일 & 필터", 10000.0, 12, "매 10,000km 또는 12개월"),
            MaintenancePreset("에어컨 필터", 10000.0, 6, "매 10,000km 또는 6개월"),
            MaintenancePreset("브레이크 패드", 30000.0, 24, "매 30,000km 또는 24개월"),
            MaintenancePreset("타이어 위치교환", 10000.0, 12, "매 10,000km 또는 12개월"),
            MaintenancePreset("배터리", 50000.0, 36, "매 50,000km 또는 36개월")
        )
    }
}
