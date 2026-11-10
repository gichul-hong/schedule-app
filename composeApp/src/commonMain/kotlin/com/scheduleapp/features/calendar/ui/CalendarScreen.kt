package com.scheduleapp.features.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.calendar.model.ScheduleEvent
import com.scheduleapp.features.calendar.viewmodel.CalendarState
import com.scheduleapp.features.calendar.viewmodel.CalendarViewMode
import com.scheduleapp.features.calendar.viewmodel.CalendarViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Month navigation & View mode switcher
            CalendarHeader(
                currentMonth = state.currentMonth,
                viewMode = state.viewMode,
                onPrevMonth = { viewModel.navigateMonth(-1) },
                onNextMonth = { viewModel.navigateMonth(1) },
                onViewModeChange = { viewModel.setViewMode(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category filter row
            CategoryFilterBar(
                categories = state.categories,
                selectedCategory = state.selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // View modes
            when (state.viewMode) {
                CalendarViewMode.MONTH -> {
                    MonthlyCalendarGrid(
                        currentMonth = state.currentMonth,
                        selectedDate = state.selectedDate,
                        events = state.rangeEvents,
                        onDateSelected = { viewModel.selectDate(it) }
                    )
                }
                CalendarViewMode.WEEK -> {
                    WeeklyCalendarStrip(
                        selectedDate = state.selectedDate,
                        events = state.rangeEvents,
                        onDateSelected = { viewModel.selectDate(it) }
                    )
                }
                CalendarViewMode.DAY -> {
                    DailyHeaderCard(selectedDate = state.selectedDate)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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

            // Selected Day Events Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${DateTimeUtils.formatDate(state.selectedDate)} ($dayOfWeekKorean) 일정 (${state.selectedDayEvents.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.selectedDayEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "등록된 일정이 없습니다. 우측 하단 '+' 버튼을 눌러 추가해보세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.selectedDayEvents, key = { it.id }) { event ->
                        EventItemCard(
                            event = event,
                            onDelete = { viewModel.deleteEvent(event) }
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddEventBottomSheet(
            selectedDate = state.selectedDate,
            onDismiss = { showAddSheet = false },
            onConfirm = { title, start, end, isAllDay, category, memo ->
                viewModel.createEvent(title, start, end, isAllDay, category, memo)
            }
        )
    }
}

@Composable
private fun CalendarHeader(
    currentMonth: LocalDate,
    viewMode: CalendarViewMode,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onViewModeChange: (CalendarViewMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Month switcher
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevMonth) {
                Text("◀", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "${currentMonth.year}년 ${currentMonth.monthNumber}월",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onNextMonth) {
                Text("▶", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        // View Mode Switcher
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(2.dp)
        ) {
            CalendarViewMode.entries.forEach { mode ->
                val isSelected = viewMode == mode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onViewModeChange(mode) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (mode) {
                            CalendarViewMode.MONTH -> "월"
                            CalendarViewMode.WEEK -> "주"
                            CalendarViewMode.DAY -> "일"
                        },
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterBar(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        categories.forEach { cat ->
            val isSelected = if (cat == "ALL") selectedCategory == null else selectedCategory == cat
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(if (cat == "ALL") null else cat) },
                label = {
                    Text(
                        text = when (cat) {
                            "ALL" -> "전체"
                            "GENERAL" -> "일반"
                            "WORK" -> "업무"
                            "PERSONAL" -> "개인"
                            "CAR" -> "차계부"
                            else -> cat
                        },
                        fontSize = 12.sp
                    )
                }
            )
        }
    }
}

@Composable
private fun MonthlyCalendarGrid(
    currentMonth: LocalDate,
    selectedDate: LocalDate,
    events: List<ScheduleEvent>,
    onDateSelected: (LocalDate) -> Unit
) {
    // Sunday first: 일, 월, 화, 수, 목, 금, 토
    val weekdayHeaders = listOf("일", "월", "화", "수", "목", "금", "토")
    val firstDayOfMonth = LocalDate(currentMonth.year, currentMonth.monthNumber, 1)
    // Sunday-first offset: Sunday (7) -> 0, Monday (1) -> 1, ..., Saturday (6) -> 6
    val startDayOffset = firstDayOfMonth.dayOfWeek.isoDayNumber % 7
    val totalDays = DateTimeUtils.endOfMonth(currentMonth.year, currentMonth.monthNumber).dayOfMonth

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Weekday labels
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdayHeaders.forEachIndexed { idx, label ->
                    val textColor = when (idx) {
                        0 -> Color(0xFFE53935) // Sunday: Red
                        6 -> Color(0xFF1E88E5) // Saturday: Blue
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Calendar day cells
            val totalCells = (startDayOffset + totalDays + 6) / 7 * 7
            for (row in 0 until totalCells / 7) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - startDayOffset + 1

                        if (dayNumber in 1..totalDays) {
                            val thisDate = LocalDate(currentMonth.year, currentMonth.monthNumber, dayNumber)
                            val isSelected = thisDate == selectedDate
                            val isToday = thisDate == DateTimeUtils.today()
                            val dayStart = DateTimeUtils.startOfDayMillis(thisDate)
                            val dayEnd = DateTimeUtils.endOfDayMillis(thisDate)
                            val hasEvents = events.any { it.startTime <= dayEnd && it.endTime >= dayStart }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primaryContainer
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { onDateSelected(thisDate) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$dayNumber",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                            col == 0 -> Color(0xFFE53935) // Sunday: Red
                                            col == 6 -> Color(0xFF1E88E5) // Saturday: Blue
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    if (hasEvents) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.primary
                                                )
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyCalendarStrip(
    selectedDate: LocalDate,
    events: List<ScheduleEvent>,
    onDateSelected: (LocalDate) -> Unit
) {
    // Current week starting from Sunday: 0=Sun .. 6=Sat
    val dayOfWeek = selectedDate.dayOfWeek.isoDayNumber // 1=Mon .. 7=Sun
    val sundayOffset = dayOfWeek % 7 // 7%7 = 0 for Sun, 1%7 = 1 for Mon, ..., 6%7 = 6 for Sat
    val sundayEpoch = selectedDate.toEpochDays() - sundayOffset
    val weekDates = (0 until 7).map { LocalDate.fromEpochDays(sundayEpoch + it) }
    val weekdayLabels = listOf("일", "월", "화", "수", "목", "금", "토")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        weekDates.forEachIndexed { index, date ->
            val isSelected = date == selectedDate
            val dayStart = DateTimeUtils.startOfDayMillis(date)
            val dayEnd = DateTimeUtils.endOfDayMillis(date)
            val hasEvents = events.any { it.startTime <= dayEnd && it.endTime >= dayStart }

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onDateSelected(date) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = weekdayLabels[index],
                    fontSize = 11.sp,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        index == 0 -> Color(0xFFE53935) // Sunday: Red
                        index == 6 -> Color(0xFF1E88E5) // Saturday: Blue
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${date.dayOfMonth}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        index == 0 -> Color(0xFFE53935) // Sunday: Red
                        index == 6 -> Color(0xFF1E88E5) // Saturday: Blue
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                if (hasEvents) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyHeaderCard(selectedDate: LocalDate) {
    val dayOfWeekKorean = when (selectedDate.dayOfWeek.isoDayNumber) {
        7 -> "일요일"
        1 -> "월요일"
        2 -> "화요일"
        3 -> "수요일"
        4 -> "목요일"
        5 -> "금요일"
        6 -> "토요일"
        else -> ""
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${selectedDate.year}년 ${selectedDate.monthNumber}월 ${selectedDate.dayOfMonth}일 ($dayOfWeekKorean)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "오늘의 집중 일정 뷰",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun EventItemCard(
    event: ScheduleEvent,
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
            // Category color stripe
            val categoryColor = when (event.category) {
                "WORK" -> Color(0xFF1E88E5)
                "PERSONAL" -> Color(0xFF43A047)
                "CAR" -> Color(0xFFFB8C00)
                else -> MaterialTheme.colorScheme.primary
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(categoryColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = categoryColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = when (event.category) {
                                "WORK" -> "업무"
                                "PERSONAL" -> "개인"
                                "CAR" -> "차계부"
                                else -> "일반"
                            },
                            color = categoryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val timeText = if (event.isAllDay) {
                    "종일"
                } else {
                    "${DateTimeUtils.formatTime(event.startTime)} - ${DateTimeUtils.formatTime(event.endTime)}"
                }
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!event.memo.isNullOrBlank()) {
                    Text(
                        text = event.memo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Text("✕", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
