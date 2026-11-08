package com.scheduleapp.features.maintenance.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.carbook.model.MaintenancePreset

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddMaintenanceDialog(
    currentVehicleOdometer: Double,
    onDismiss: () -> Unit,
    onConfirm: (partName: String, cycleKm: Double, cycleMonths: Int, lastDate: Long, lastOdo: Double) -> Unit
) {
    var partName by remember { mutableStateOf("") }
    var cycleKmText by remember { mutableStateOf("10000") }
    var cycleMonthsText by remember { mutableStateOf("12") }
    var lastOdoText by remember { mutableStateOf(currentVehicleOdometer.toLong().toString()) }
    var lastDateText by remember { mutableStateOf(DateTimeUtils.formatDate(DateTimeUtils.today())) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("소모품 / 정비 주기 추가", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("자주 쓰는 소모품 추천:", style = MaterialTheme.typography.labelMedium)

                // Presets FlowRow
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MaintenancePreset.DEFAULT_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = partName == preset.partName,
                            onClick = {
                                partName = preset.partName
                                cycleKmText = preset.cycleKm.toLong().toString()
                                cycleMonthsText = preset.cycleMonths.toString()
                            },
                            label = { Text(preset.partName, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = partName,
                    onValueChange = { partName = it },
                    label = { Text("소모품 / 부품명 *") },
                    placeholder = { Text("예: 엔진오일, 브레이크 패드") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = cycleKmText,
                        onValueChange = { cycleKmText = it },
                        label = { Text("교체 주기 (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cycleMonthsText,
                        onValueChange = { cycleMonthsText = it },
                        label = { Text("교체 주기 (개월)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = lastOdoText,
                    onValueChange = { lastOdoText = it },
                    label = { Text("최근 교체 주행거리 (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = lastDateText,
                    onValueChange = { lastDateText = it },
                    label = { Text("최근 교체 일자 (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val name = partName.trim()
                    if (name.isBlank()) {
                        errorMessage = "부품명을 입력해주세요."
                        return@Button
                    }
                    val cycleKm = cycleKmText.toDoubleOrNull() ?: 0.0
                    val cycleMonths = cycleMonthsText.toIntOrNull() ?: 0
                    if (cycleKm <= 0.0 && cycleMonths <= 0) {
                        errorMessage = "교체 주기(km 또는 개월)를 1개 이상 입력해주세요."
                        return@Button
                    }

                    val lastOdo = lastOdoText.toDoubleOrNull() ?: 0.0
                    val lastDate = try {
                        DateTimeUtils.startOfDayMillis(DateTimeUtils.parseDate(lastDateText.trim()))
                    } catch (e: Exception) {
                        DateTimeUtils.nowMillis()
                    }

                    onConfirm(name, cycleKm, cycleMonths, lastDate, lastOdo)
                    onDismiss()
                }
            ) {
                Text("추가")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
