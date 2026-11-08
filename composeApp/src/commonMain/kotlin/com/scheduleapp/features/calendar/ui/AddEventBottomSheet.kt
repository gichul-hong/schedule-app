package com.scheduleapp.features.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scheduleapp.core.util.DateTimeUtils
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventBottomSheet(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (title: String, startTime: Long, endTime: Long, isAllDay: Boolean, category: String, memo: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var isAllDay by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("GENERAL") }
    val categories = listOf("GENERAL", "WORK", "PERSONAL", "CAR")

    val startOfDay = remember(selectedDate) { DateTimeUtils.startOfDayMillis(selectedDate) }
    val endOfDay = remember(selectedDate) { DateTimeUtils.endOfDayMillis(selectedDate) }

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
        ) {
            Text(
                text = "새 일정 등록",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("일정 제목") },
                placeholder = { Text("회의, 약속, 정비 등") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Date indicator & All Day Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "날짜: ${DateTimeUtils.formatDate(selectedDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("종일", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isAllDay,
                        onCheckedChange = { isAllDay = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category selector
            Text(
                text = "카테고리",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = {
                            Text(
                                text = when (cat) {
                                    "GENERAL" -> "일반"
                                    "WORK" -> "업무"
                                    "PERSONAL" -> "개인"
                                    "CAR" -> "차계부"
                                    else -> cat
                                }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Memo input
            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text("메모 (선택)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

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
                        if (title.isNotBlank()) {
                            val start = if (isAllDay) startOfDay else startOfDay + 9 * 3600 * 1000L // 09:00
                            val end = if (isAllDay) endOfDay else startOfDay + 10 * 3600 * 1000L // 10:00
                            onConfirm(title, start, end, isAllDay, selectedCategory, memo.ifBlank { null })
                            onDismiss()
                        }
                    },
                    enabled = title.isNotBlank()
                ) {
                    Text("저장")
                }
            }
        }
    }
}
