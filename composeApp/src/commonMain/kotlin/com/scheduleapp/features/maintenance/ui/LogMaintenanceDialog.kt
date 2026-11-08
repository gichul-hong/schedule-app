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
import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.carbook.model.MaintenanceItem

@Composable
fun LogMaintenanceDialog(
    item: MaintenanceItem,
    currentVehicleOdometer: Double,
    onDismiss: () -> Unit,
    onConfirm: (serviceDate: Long, serviceOdometer: Double, cost: Double, memo: String?) -> Unit
) {
    var dateText by remember { mutableStateOf(DateTimeUtils.formatDate(DateTimeUtils.today())) }
    var odometerText by remember { mutableStateOf(currentVehicleOdometer.toLong().toString()) }
    var costText by remember { mutableStateOf("") }
    var memoText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("정비 / 소모품 교체 완료", fontWeight = FontWeight.Bold)
                Text(
                    text = item.partName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "정비 기록 시 소모품 교체 주기가 초기화되며 차계부 지출 내역 및 캘린더 일정이 자동으로 갱신됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("교체 일자 (YYYY-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = odometerText,
                    onValueChange = { odometerText = it },
                    label = { Text("교체 시 주행거리 (km) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("정비 비용 (원)") },
                    placeholder = { Text("예: 120000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = memoText,
                    onValueChange = { memoText = it },
                    label = { Text("메모 (정비소, 부품 규격 등)") },
                    placeholder = { Text("예: 공식 서비스센터 순정품 교체") },
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
                    val odo = odometerText.toDoubleOrNull()
                    if (odo == null || odo < 0.0) {
                        errorMessage = "올바른 주행거리를 입력해주세요."
                        return@Button
                    }
                    if (odo < currentVehicleOdometer) {
                        errorMessage = "입력한 주행거리가 현재 주행거리(${currentVehicleOdometer.toLong()}km)보다 작습니다."
                        return@Button
                    }

                    val serviceDate = try {
                        DateTimeUtils.startOfDayMillis(DateTimeUtils.parseDate(dateText.trim()))
                    } catch (e: Exception) {
                        DateTimeUtils.nowMillis()
                    }

                    val cost = costText.toDoubleOrNull() ?: 0.0
                    val memo = memoText.trim().ifBlank { null }

                    onConfirm(serviceDate, odo, cost, memo)
                    onDismiss()
                }
            ) {
                Text("기록 완료")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
