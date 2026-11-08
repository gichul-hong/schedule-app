package com.scheduleapp.features.carbook.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.carbook.model.ExpenseType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseBottomSheet(
    currentVehicleOdometer: Double,
    onDismiss: () -> Unit,
    onConfirm: (
        type: ExpenseType,
        amount: Double,
        odometer: Double?,
        date: Long,
        memo: String?
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf(ExpenseType.MAINTENANCE) }
    var amountText by remember { mutableStateOf("") }
    var odoText by remember {
        mutableStateOf(if (currentVehicleOdometer > 0) currentVehicleOdometer.toInt().toString() else "")
    }
    var memo by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val todayDate = remember { DateTimeUtils.today() }
    val recordDateMillis = remember { DateTimeUtils.nowMillis() }
    val categoryScrollState = rememberScrollState()
    val contentScrollState = rememberScrollState()

    // Non-fuel expense categories (fuel has dedicated bottom sheet)
    val expenseCategories = remember {
        ExpenseType.entries.filter { it != ExpenseType.FUEL }
    }

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
                .verticalScroll(contentScrollState)
        ) {
            Text(
                text = "차량 지출 등록 💳",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Current Date indicator
            Text(
                text = "지출 일자: ${DateTimeUtils.formatDate(todayDate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Chips (Horizontal scrollable)
            Text(
                text = "지출 항목",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(categoryScrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                expenseCategories.forEach { cat ->
                    FilterChip(
                        selected = selectedType == cat,
                        onClick = { selectedType = cat },
                        label = { Text("${cat.icon} ${cat.displayName}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it.filter { char -> char.isDigit() }
                    validationError = null
                },
                label = { Text("지출 금액 (원)") },
                placeholder = { Text("예: 85000") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Odometer Input (Optional)
            OutlinedTextField(
                value = odoText,
                onValueChange = {
                    odoText = it.filter { char -> char.isDigit() || char == '.' }
                    validationError = null
                },
                label = { Text("지출 시 주행거리 (km, 선택)") },
                placeholder = { Text("현재 ${currentVehicleOdometer.toInt()} km") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Memo Input
            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text("상세 내용 / 메모") },
                placeholder = { Text("예: 엔진오일 및 오일필터 교체, 공영주차장") },
                minLines = 2,
                maxLines = 3,
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
                        val amt = amountText.toDoubleOrNull()
                        val odo = odoText.toDoubleOrNull()

                        if (amt == null || amt <= 0) {
                            validationError = "올바른 지출 금액을 입력해주세요."
                            return@Button
                        }
                        if (odo != null && odo < currentVehicleOdometer) {
                            validationError = "주행거리는 현재 누적 주행거리(${currentVehicleOdometer.toInt()} km) 이상이어야 합니다."
                            return@Button
                        }

                        onConfirm(selectedType, amt, odo, recordDateMillis, memo.ifBlank { null })
                        onDismiss()
                    },
                    enabled = amountText.isNotBlank()
                ) {
                    Text("지출 기록 저장")
                }
            }
        }
    }
}
