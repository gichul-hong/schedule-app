package com.scheduleapp.features.timeline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.calendar.model.Priority
import com.scheduleapp.features.timeline.viewmodel.TimelineItem
import com.scheduleapp.features.timeline.viewmodel.TimelineViewModel
import kotlinx.datetime.isoDayNumber

@Composable
fun IntegratedTimelineScreen(
    viewModel: TimelineViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Screen Header & Day Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "통합 타임라인",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateDay(-1) }) {
                        Text("◀", fontSize = 14.sp)
                    }
                    val isToday = state.selectedDate == DateTimeUtils.today()
                    val dayOfWeekKorean = when (state.selectedDate.dayOfWeek.isoDayNumber) {
                        7 -> "일"
                        1 -> "월"
                        2 -> "화"
                        3 -> "수"
                        4 -> "목"
                        5 -> "금"
                        6 -> "토"
                        else -> ""
                    }
                    Text(
                        text = if (isToday) "오늘 (${state.selectedDate.monthNumber}/${state.selectedDate.dayOfMonth} $dayOfWeekKorean)"
                        else "${state.selectedDate.monthNumber}월 ${state.selectedDate.dayOfMonth}일 ($dayOfWeekKorean)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.navigateDay(1) }) {
                        Text("▶", fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryChip(
                    label = "일정",
                    count = state.eventCount,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                SummaryChip(
                    label = "할 일",
                    count = state.todoCount,
                    color = Color(0xFF43A047),
                    modifier = Modifier.weight(1f)
                )
                SummaryChip(
                    label = "차량 알림",
                    count = state.carAlertCount,
                    color = Color(0xFFFB8C00),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "오늘 예정된 일정, 할 일 또는 차량 알림이 없습니다.\n편안하고 활기찬 하루 보내세요!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.items) { item ->
                        when (item) {
                            is TimelineItem.EventItem -> {
                                TimelineEventCard(
                                    item = item,
                                    onDelete = { viewModel.deleteEvent(item.event) }
                                )
                            }
                            is TimelineItem.TodoTimelineItem -> {
                                TimelineTodoCard(
                                    item = item,
                                    onToggle = { completed -> viewModel.toggleTodo(item.todo.id, completed) }
                                )
                            }
                            is TimelineItem.CarAlertItem -> {
                                TimelineCarAlertCard(item = item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimelineEventCard(
    item: TimelineItem.EventItem,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon / Indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("📅", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.event.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "일정",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val timeStr = if (item.event.isAllDay) "종일"
                else "${DateTimeUtils.formatTime(item.event.startTime)} - ${DateTimeUtils.formatTime(item.event.endTime)}"
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Text("✕", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun TimelineTodoCard(
    item: TimelineItem.TodoTimelineItem,
    onToggle: (Boolean) -> Unit
) {
    val priorityColor = when (item.todo.priority) {
        Priority.URGENT -> Color(0xFFE53935)
        Priority.HIGH -> Color(0xFFFB8C00)
        Priority.MEDIUM -> Color(0xFF1E88E5)
        Priority.LOW -> Color(0xFF757575)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.todo.isCompleted,
                onCheckedChange = { onToggle(it) }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.todo.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (item.todo.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                        textDecoration = if (item.todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.todo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = priorityColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.todo.priority.displayName,
                            color = priorityColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        color = Color(0xFF43A047).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "할 일",
                            color = Color(0xFF2E7D32),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineCarAlertCard(
    item: TimelineItem.CarAlertItem
) {
    val alertColor = if (item.isOverdue) Color(0xFFE53935) else Color(0xFFFB8C00)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = alertColor.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(alertColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🚗", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.message,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = alertColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${item.vehicleName} · ${item.maintenanceItem.partName} 점검/교체 필요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = alertColor,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (item.isOverdue) "기한 초과" else "예정",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
