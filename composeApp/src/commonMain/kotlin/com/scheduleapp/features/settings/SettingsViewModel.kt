package com.scheduleapp.features.settings

import androidx.lifecycle.ViewModel
import com.scheduleapp.core.platform.DatabaseBackupEngine
import com.scheduleapp.core.platform.GitHubSyncEngine
import com.scheduleapp.core.theme.ThemeMode
import com.scheduleapp.domain.carbook.model.Vehicle
import com.scheduleapp.domain.carbook.repository.CarBookRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val carBookRepository: CarBookRepository,
    private val backupEngine: DatabaseBackupEngine,
    private val gitHubSyncEngine: GitHubSyncEngine? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    init {
        loadVehicles()
    }

    private fun loadVehicles() {
        scope.launch {
            carBookRepository.getAllVehicles().collect { vehicles ->
                val defaultCar = vehicles.find { it.isDefault } ?: vehicles.firstOrNull()
                _uiState.update { it.copy(vehicles = vehicles, defaultVehicle = defaultCar) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        _uiState.update { it.copy(distanceUnit = unit) }
    }

    fun setVolumeUnit(unit: VolumeUnit) {
        _uiState.update { it.copy(volumeUnit = unit) }
    }

    fun setDefaultVehicle(vehicleId: Long) {
        scope.launch {
            val currentVehicles = _uiState.value.vehicles
            currentVehicles.forEach { vehicle ->
                val shouldBeDefault = vehicle.id == vehicleId
                if (vehicle.isDefault != shouldBeDefault) {
                    carBookRepository.updateVehicle(vehicle.copy(isDefault = shouldBeDefault))
                }
            }
            val newDefault = currentVehicles.find { it.id == vehicleId }
            _uiState.update { it.copy(defaultVehicle = newDefault) }
        }
    }

    fun exportBackup() {
        scope.launch {
            _uiState.update { it.copy(isExporting = true, statusMessage = null) }
            try {
                val json = backupEngine.exportToJson()
                val snapshot = DatabaseBackupEngine.parseSnapshot(json)
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportedJson = json,
                        statusMessage = "백업 스냅샷이 생성되었습니다. (총 ${snapshot.totalRecords}개 데이터 항목)",
                        isStatusError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        statusMessage = "백업 내보내기 실패: ${e.message}",
                        isStatusError = true
                    )
                }
            }
        }
    }

    fun restoreBackup(json: String) {
        scope.launch {
            _uiState.update { it.copy(isRestoring = true, statusMessage = null) }
            val result = backupEngine.restoreFromJson(json)
            result.fold(
                onSuccess = { snapshot ->
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            statusMessage = "성공적으로 복원되었습니다. (총 ${snapshot.totalRecords}개 데이터 복원 완료)",
                            isStatusError = false,
                            exportedJson = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            statusMessage = "데이터 복원 실패: ${error.message}",
                            isStatusError = true
                        )
                    }
                }
            )
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null, exportedJson = null) }
    }

    fun updateGitHubConfig(pat: String, owner: String, repo: String, branch: String) {
        _uiState.update {
            it.copy(
                githubPat = pat,
                githubOwner = owner,
                githubRepo = repo,
                githubBranch = branch
            )
        }
    }

    fun pushToGitHub() {
        val engine = gitHubSyncEngine ?: return
        val current = _uiState.value
        scope.launch {
            _uiState.update { it.copy(isGitHubSyncing = true, statusMessage = null) }
            val result = engine.pushBackup(
                pat = current.githubPat.trim(),
                owner = current.githubOwner.trim(),
                repo = current.githubRepo.trim(),
                branch = current.githubBranch.trim()
            )
            result.fold(
                onSuccess = { syncRes ->
                    val imgText = if (syncRes.imagesCount > 0) ", 영수증 사진 ${syncRes.imagesCount}장 업로드" else ""
                    _uiState.update {
                        it.copy(
                            isGitHubSyncing = false,
                            statusMessage = "GitHub 백업 완료! (데이터 ${syncRes.recordsCount}건${imgText})",
                            isStatusError = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isGitHubSyncing = false,
                            statusMessage = error.message ?: "GitHub 백업 실패",
                            isStatusError = true
                        )
                    }
                }
            )
        }
    }

    fun pullFromGitHub() {
        val engine = gitHubSyncEngine ?: return
        val current = _uiState.value
        scope.launch {
            _uiState.update { it.copy(isGitHubSyncing = true, statusMessage = null) }
            val result = engine.pullBackup(
                pat = current.githubPat.trim(),
                owner = current.githubOwner.trim(),
                repo = current.githubRepo.trim(),
                branch = current.githubBranch.trim()
            )
            result.fold(
                onSuccess = { syncRes ->
                    val imgText = if (syncRes.imagesCount > 0) ", 영수증 사진 ${syncRes.imagesCount}장 다운로드" else ""
                    _uiState.update {
                        it.copy(
                            isGitHubSyncing = false,
                            statusMessage = "GitHub에서 복원 완료! (데이터 ${syncRes.recordsCount}건${imgText})",
                            isStatusError = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isGitHubSyncing = false,
                            statusMessage = error.message ?: "GitHub 복원 실패",
                            isStatusError = true
                        )
                    }
                }
            )
        }
    }
}
