package com.scheduleapp.features.carbook.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scheduleapp.domain.carbook.model.FuelType
import com.scheduleapp.domain.carbook.model.Vehicle

@Composable
fun VehicleProfileDialog(
    initialVehicle: Vehicle? = null,
    onDismiss: () -> Unit,
    onSave: (Vehicle) -> Unit
) {
    var name by remember { mutableStateOf(initialVehicle?.name ?: "") }
    var model by remember { mutableStateOf(initialVehicle?.model ?: "") }
    var selectedFuelType by remember { mutableStateOf(initialVehicle?.fuelType ?: FuelType.GASOLINE) }
    var initialOdoText by remember {
        mutableStateOf(initialVehicle?.initialOdometer?.let { if (it > 0) it.toInt().toString() else "" } ?: "")
    }
    var currentOdoText by remember {
        mutableStateOf(initialVehicle?.currentOdometer?.let { if (it > 0) it.toInt().toString() else "" } ?: "")
    }
    var isDefault by remember { mutableStateOf(initialVehicle?.isDefault ?: true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialVehicle == null || initialVehicle.id == 0L) "차량 등록" else "차량 정보 수정",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("차량 별칭") },
                    placeholder = { Text("예: 패밀리카, 출퇴근차") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("차종 / 모델명") },
                    placeholder = { Text("예: 아반떼 CN7, 아이오닉 5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "연료 종류",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FuelType.entries.forEach { fType ->
                        FilterChip(
                            selected = selectedFuelType == fType,
                            onClick = { selectedFuelType = fType },
                            label = { Text(fType.displayName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = initialOdoText,
                    onValueChange = { initialOdoText = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("최초 등록 주행거리 (km)") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = currentOdoText,
                    onValueChange = { currentOdoText = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("현재 누적 주행거리 (km)") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || model.isBlank()) {
                        validationError = "별칭과 모델명을 입력해주세요."
                        return@Button
                    }
                    val initOdo = initialOdoText.toDoubleOrNull() ?: 0.0
                    val currOdo = currentOdoText.toDoubleOrNull() ?: initOdo
                    if (currOdo < initOdo) {
                        validationError = "현재 주행거리는 최초 주행거리보다 작을 수 없습니다."
                        return@Button
                    }

                    val updated = (initialVehicle ?: Vehicle(name = name, model = model)).copy(
                        name = name.trim(),
                        model = model.trim(),
                        fuelType = selectedFuelType,
                        initialOdometer = initOdo,
                        currentOdometer = currOdo,
                        isDefault = isDefault
                    )
                    onSave(updated)
                    onDismiss()
                },
                enabled = name.isNotBlank() && model.isNotBlank()
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
