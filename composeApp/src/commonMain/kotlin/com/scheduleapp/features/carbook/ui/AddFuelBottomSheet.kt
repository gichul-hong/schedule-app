package com.scheduleapp.features.carbook.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scheduleapp.core.util.DateTimeUtils
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFuelBottomSheet(
    currentVehicleOdometer: Double,
    onDismiss: () -> Unit,
    onConfirm: (
        amount: Double,
        liters: Double,
        unitPrice: Double,
        odometer: Double,
        isFullTank: Boolean,
        date: Long,
        memo: String?
    ) -> Unit
) {
    var odoText by remember {
        mutableStateOf(if (currentVehicleOdometer > 0) currentVehicleOdometer.toInt().toString() else "")
    }
    var amountText by remember { mutableStateOf("") }
    var litersText by remember { mutableStateOf("") }
    var unitPriceText by remember { mutableStateOf("") }
    var isFullTank by remember { mutableStateOf(true) }
    var memo by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val todayDate = remember { DateTimeUtils.today() }
    val recordDateMillis = remember { DateTimeUtils.nowMillis() }
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "주유 기록 등록 ⛽",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Current Date indicator
            Text(
                text = "주유 일자: ${DateTimeUtils.formatDate(todayDate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Odometer Input
            OutlinedTextField(
                value = odoText,
                onValueChange = {
                    odoText = it.filter { char -> char.isDigit() || char == '.' }
                    validationError = null
                },
                label = { Text("주유 시 주행거리 (km)") },
                placeholder = { Text("현재 ${currentVehicleOdometer.toInt()} km 이상") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Amount (KRW) Input
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it.filter { char -> char.isDigit() }
                    validationError = null
                    val amt = amountText.toDoubleOrNull()
                    val ltr = litersText.toDoubleOrNull()
                    if (amt != null && ltr != null && ltr > 0.0) {
                        unitPriceText = (amt / ltr).roundToInt().toString()
                    }
                },
                label = { Text("주유 금액 (원)") },
                placeholder = { Text("예: 70000") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Liters & Unit Price in a Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = litersText,
                    onValueChange = {
                        litersText = it.filter { char -> char.isDigit() || char == '.' }
                        validationError = null
                        val amt = amountText.toDoubleOrNull()
                        val ltr = litersText.toDoubleOrNull()
                        if (amt != null && ltr != null && ltr > 0.0) {
                            unitPriceText = (amt / ltr).roundToInt().toString()
                        }
                    },
                    label = { Text("주유량 (L)") },
                    placeholder = { Text("예: 40.0") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = unitPriceText,
                    onValueChange = {
                        unitPriceText = it.filter { char -> char.isDigit() || char == '.' }
                        validationError = null
                        val amt = amountText.toDoubleOrNull()
                        val price = unitPriceText.toDoubleOrNull()
                        if (amt != null && price != null && price > 0.0) {
                            litersText = ((amt / price * 100).roundToInt() / 100.0).toString()
                        }
                    },
                    label = { Text("단가 (원/L)") },
                    placeholder = { Text("예: 1750") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Full-tank Toggle
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "가득 주유 여부 (Full Tank)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isFullTank) "가득 주유 완료 (정확한 Full-to-Full 연비 산출 가능)" else "일부 주유 (다음 가득 주유 시 합산 연비 계산)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isFullTank,
                        onCheckedChange = { isFullTank = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Memo Input
            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text("메모 (주유소 이름, 세차 여부 등)") },
                placeholder = { Text("예: GS칼텍스 강남점") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (validationError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val odo = odoText.toDoubleOrNull()
                        val amt = amountText.toDoubleOrNull()
                        val ltr = litersText.toDoubleOrNull()
                        val price = unitPriceText.toDoubleOrNull() ?: if (amt != null && ltr != null && ltr > 0) amt / ltr else null

                        if (odo == null || amt == null || ltr == null || price == null) {
                            validationError = "주행거리, 금액, 주유량을 모두 올바르게 입력해주세요."
                            return@Button
                        }
                        if (odo < currentVehicleOdometer) {
                            validationError = "주행거리는 현재 누적 주행거리(${currentVehicleOdometer.toInt()} km) 이상이어야 합니다."
                            return@Button
                        }
                        if (amt <= 0 || ltr <= 0) {
                            validationError = "금액과 주유량은 0보다 커야 합니다."
                            return@Button
                        }

                        onConfirm(amt, ltr, price, odo, isFullTank, recordDateMillis, memo.ifBlank { null })
                        onDismiss()
                    },
                    enabled = amountText.isNotBlank() && litersText.isNotBlank() && odoText.isNotBlank()
                ) {
                    Text("주유 기록 저장")
                }
            }
        }
    }
}
