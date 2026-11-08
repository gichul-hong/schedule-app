package com.scheduleapp.features.routine.ui

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
import com.scheduleapp.domain.calendar.engine.RoutineStreakEngine

@Composable
fun AddRoutineDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, repeatDaysMask: Int, targetTime: String?, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("DAILY") }
    // Default repeatDaysMask = 127 (Every day: Mon to Sun)
    var repeatDaysMask by remember { mutableStateOf(127) }
    var targetTime by remember { mutableStateOf("") }

    val weekdayLabels = listOf("월", "화", "수", "목", "금", "토", "일")
    val categories = listOf("DAILY", "HEALTH", "STUDY", "WORK")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "새 루틴(습관) 등록",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("루틴 이름") },
                    placeholder = { Text("예: 물 2L 마시기, 매일 독서") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Repeat Day Selection
                Text(
                    text = "반복 요일 설정",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekdayLabels.forEachIndexed { index, label ->
                        val dayNumber = index + 1 // 1 = Mon ... 7 = Sun
                        val isSelected = RoutineStreakEngine.isScheduledOn(repeatDaysMask, dayNumber)

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    val bit = 1 shl (dayNumber - 1)
                                    repeatDaysMask = repeatDaysMask xor bit
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category selector
                Text(
                    text = "카테고리",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    when (cat) {
                                        "DAILY" -> "일상"
                                        "HEALTH" -> "건강"
                                        "STUDY" -> "학습"
                                        "WORK" -> "업무"
                                        else -> cat
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Target time (optional)
                OutlinedTextField(
                    value = targetTime,
                    onValueChange = { targetTime = it },
                    label = { Text("목표 시간 (선택)") },
                    placeholder = { Text("예: 07:00, 취침 전") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && repeatDaysMask > 0) {
                        onConfirm(
                            title,
                            repeatDaysMask,
                            targetTime.ifBlank { null },
                            selectedCategory
                        )
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank() && repeatDaysMask > 0
            ) {
                Text("등록")
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
