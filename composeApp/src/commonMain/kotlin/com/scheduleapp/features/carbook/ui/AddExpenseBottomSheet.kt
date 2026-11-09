package com.scheduleapp.features.carbook.ui

import androidx.compose.foundation.horizontalScroll
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
import com.scheduleapp.core.platform.ReceiptImageThumbnail
import com.scheduleapp.core.platform.rememberReceiptImagePicker
import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.carbook.model.CarExpenseLog
import com.scheduleapp.domain.carbook.model.ExpenseType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseBottomSheet(
    currentVehicleOdometer: Double,
    initialExpense: CarExpenseLog? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        type: ExpenseType,
        amount: Double,
        odometer: Double?,
        date: Long,
        memo: String?,
        receiptImageUri: String?
    ) -> Unit
) {
    val isEditMode = initialExpense != null

    var selectedType by remember(initialExpense) {
        mutableStateOf(
            if (initialExpense != null && initialExpense.type != ExpenseType.FUEL) {
                initialExpense.type
            } else {
                ExpenseType.MAINTENANCE
            }
        )
    }
    var amountText by remember(initialExpense) {
        mutableStateOf(
            initialExpense?.amount?.toLong()?.toString() ?: ""
        )
    }
    var dateText by remember(initialExpense) {
        val initDate = if (initialExpense != null) {
            DateTimeUtils.localDateFromMillis(initialExpense.date)
        } else {
            DateTimeUtils.today()
        }
        mutableStateOf(DateTimeUtils.formatDate(initDate))
    }
    var odoText by remember(initialExpense) {
        val initOdo = initialExpense?.odometer ?: currentVehicleOdometer
        mutableStateOf(if (initOdo > 0) initOdo.toInt().toString() else "")
    }
    var memo by remember(initialExpense) {
        mutableStateOf(initialExpense?.memo ?: "")
    }
    var receiptImageUri by remember(initialExpense) {
        mutableStateOf(initialExpense?.receiptImageUri)
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    val launchReceiptPicker = rememberReceiptImagePicker { pickedPath ->
        receiptImageUri = pickedPath
    }

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
                text = if (isEditMode) "지출 내역 수정 ✏️" else "차량 지출 등록 💳",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

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

            Spacer(modifier = Modifier.height(12.dp))

            // Date Input (YYYY-MM-DD)
            OutlinedTextField(
                value = dateText,
                onValueChange = {
                    dateText = it.filter { ch -> ch.isDigit() || ch == '-' }
                    validationError = null
                },
                label = { Text("지출 일자 (YYYY-MM-DD)") },
                placeholder = { Text("예: 2026-11-09") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it.filter { char -> char.isDigit() }
                    validationError = null
                },
                label = { Text("지출 금액 (원, 0원 입력 가능)") },
                placeholder = { Text("예: 690000") },
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
                label = { Text("지출 시 주행거리 (km)") },
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
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Receipt Image Attachment Section
            Text(
                text = "영수증 이미지 첨부",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { launchReceiptPicker() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (receiptImageUri == null) "📷 영수증 사진 첨부" else "📷 영수증 사진 변경")
                }

                if (receiptImageUri != null) {
                    TextButton(
                        onClick = { receiptImageUri = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("삭제")
                    }
                }
            }

            if (receiptImageUri != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ReceiptImageThumbnail(
                    imageUri = receiptImageUri!!,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }

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
                        val amt = if (amountText.isBlank()) 0.0 else amountText.toDoubleOrNull()
                        val odo = odoText.toDoubleOrNull()
                        val parsedDate = runCatching { DateTimeUtils.parseDate(dateText.trim()) }.getOrNull()

                        if (parsedDate == null) {
                            validationError = "날짜 형식을 확인해주세요 (예: 2026-11-09)."
                            return@Button
                        }
                        if (amt == null || amt < 0) {
                            validationError = "올바른 지출 금액을 입력해주세요."
                            return@Button
                        }
                        if (odo != null && odo < 0) {
                            validationError = "올바른 주행거리를 입력해주세요."
                            return@Button
                        }

                        val dateMillis = DateTimeUtils.startOfDayMillis(parsedDate)
                        onConfirm(
                            selectedType,
                            amt,
                            odo,
                            dateMillis,
                            memo.ifBlank { null },
                            receiptImageUri
                        )
                        onDismiss()
                    }
                ) {
                    Text(if (isEditMode) "수정 내용 저장" else "지출 기록 저장")
                }
            }
        }
    }
}
