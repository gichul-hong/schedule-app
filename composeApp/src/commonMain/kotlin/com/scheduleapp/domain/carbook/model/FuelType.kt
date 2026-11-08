package com.scheduleapp.domain.carbook.model

enum class FuelType(val displayName: String) {
    GASOLINE("휘발유"),
    DIESEL("경유"),
    HYBRID("하이브리드"),
    ELECTRIC("전기"),
    LPG("LPG");

    companion object {
        fun fromString(value: String): FuelType =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: GASOLINE
    }
}
