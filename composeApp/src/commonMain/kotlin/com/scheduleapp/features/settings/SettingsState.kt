package com.scheduleapp.features.settings

import com.scheduleapp.core.theme.ThemeMode
import com.scheduleapp.domain.carbook.model.Vehicle

enum class DistanceUnit(val label: String, val symbol: String) {
    KM("킬로미터", "km"),
    MILE("마일", "mi")
}

enum class VolumeUnit(val label: String, val symbol: String) {
    LITER("리터", "L"),
    GALLON("갤런", "gal")
}

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val volumeUnit: VolumeUnit = VolumeUnit.LITER,
    val vehicles: List<Vehicle> = emptyList(),
    val defaultVehicle: Vehicle? = null,
    val isExporting: Boolean = false,
    val isRestoring: Boolean = false,
    val exportedJson: String? = null,
    val statusMessage: String? = null,
    val isStatusError: Boolean = false,
    val githubPat: String = "",
    val githubOwner: String = "gichul-hong",
    val githubRepo: String = "schedule-app",
    val githubBranch: String = "main",
    val isGitHubSyncing: Boolean = false,
    val appVersion: String = "1.0.0-MVP",
    val buildNumber: String = "2026.11.08"
)
