package com.scheduleapp.features.reports.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scheduleapp.domain.carbook.engine.ExpenseCategorySlice
import com.scheduleapp.domain.carbook.engine.MonthlyExpenseBarData
import com.scheduleapp.features.reports.viewmodel.ExpenseReportViewModel
import com.scheduleapp.features.reports.viewmodel.VehicleSpendShare
import kotlin.math.round

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseReportScreen(
    viewModel: ExpenseReportViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Multi-Vehicle Scope Selector: Selected Vehicle vs Combined All Vehicles
            if (state.vehicles.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !state.isAllVehiclesMode,
                        onClick = { viewModel.setAllVehiclesMode(false) },
                        label = {
                            Text(
                                text = "🚗 ${state.vehicle?.name ?: "선택 차량"} 개별",
                                fontWeight = if (!state.isAllVehiclesMode) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    FilterChip(
                        selected = state.isAllVehiclesMode,
                        onClick = { viewModel.setAllVehiclesMode(true) },
                        label = {
                            Text(
                                text = "🚘 전체 차량 합산 (${state.vehicles.size}대 통합)",
                                fontWeight = if (state.isAllVehiclesMode) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Year & Month Filter Controls
            YearAndMonthSelector(
                selectedYear = state.selectedYear,
                selectedMonth = state.selectedMonth,
                availableYears = state.availableYears,
                onYearSelected = { viewModel.selectYear(it) },
                onMonthSelected = { viewModel.selectMonth(it) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Summary Metric Cards
            ExpenseSummaryCards(
                scopeLabel = if (state.isAllVehiclesMode) "전체 차량(${state.vehicles.size}대) 합산" else (state.vehicle?.name ?: "선택 차량"),
                totalSpend = state.summary.totalSpend,
                monthlyAverage = state.summary.monthlyAverage,
                fuelTotal = state.summary.fuelTotal,
                maintenanceTotal = state.summary.maintenanceTotal,
                fixedCostsTotal = state.summary.fixedCostsTotal,
                isMonthSelected = state.selectedMonth != null
            )

            // Per-Vehicle Spend Comparison Card (when 2+ cars exist)
            if (state.vehicleSpendComparison.size > 1) {
                Spacer(modifier = Modifier.height(16.dp))
                VehicleComparisonCard(
                    comparisons = state.vehicleSpendComparison,
                    selectedVehicleId = if (state.isAllVehiclesMode) null else state.vehicle?.id,
                    onSelectVehicle = { vehicle ->
                        viewModel.setAllVehiclesMode(false)
                        viewModel.selectVehicle(vehicle)
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Monthly Spend Trend Bar Chart Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📊 ${state.selectedYear}년 월별 지출 추이",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "막대를 탭하여 해당 월의 지출 세부 비중을 확인할 수 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    MonthlyTrendBarChart(
                        monthlyTrend = state.monthlyTrend,
                        selectedMonth = state.selectedMonth,
                        onMonthClick = { month ->
                            if (state.selectedMonth == month) {
                                viewModel.selectMonth(null)
                            } else {
                                viewModel.selectMonth(month)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Category Breakdown Donut Chart Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val periodTitle = if (state.selectedMonth != null) {
                        "${state.selectedYear}년 ${state.selectedMonth}월"
                    } else {
                        "${state.selectedYear}년 연간"
                    }

                    Text(
                        text = "🍩 $periodTitle 카테고리별 지출 비중",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.categoryBreakdown.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "해당 기간의 지출 기록이 없습니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        CategoryDonutChart(
                            slices = state.categoryBreakdown,
                            totalSpend = state.summary.totalSpend
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Category Breakdown Legend List
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.categoryBreakdown.forEach { slice ->
                                CategoryLegendRow(slice = slice)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun YearAndMonthSelector(
    selectedYear: Int,
    selectedMonth: Int?,
    availableYears: List<Int>,
    onYearSelected: (Int) -> Unit,
    onMonthSelected: (Int?) -> Unit
) {
    Column {
        // Year Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            availableYears.forEach { year ->
                FilterChip(
                    selected = selectedYear == year,
                    onClick = { onYearSelected(year) },
                    label = { Text("${year}년", fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Month Selector (Horizontal Scroll)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedMonth == null,
                onClick = { onMonthSelected(null) },
                label = { Text("전체") }
            )
            (1..12).forEach { month ->
                FilterChip(
                    selected = selectedMonth == month,
                    onClick = { onMonthSelected(month) },
                    label = { Text("${month}월") }
                )
            }
        }
    }
}

@Composable
private fun ExpenseSummaryCards(
    scopeLabel: String,
    totalSpend: Double,
    monthlyAverage: Double,
    fuelTotal: Double,
    maintenanceTotal: Double,
    fixedCostsTotal: Double,
    isMonthSelected: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "$scopeLabel • ${if (isMonthSelected) "선택 월 총 유지비" else "연간 총 유지비"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatCurrency(totalSpend),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (!isMonthSelected) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "월 평균 지출: ${formatCurrency(monthlyAverage)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Sub-metrics: Fuel vs Maintenance vs Fixed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SubStatColumn(title = "주유비", amount = fuelTotal, icon = "⛽")
                SubStatColumn(title = "정비/수리", amount = maintenanceTotal, icon = "🔧")
                SubStatColumn(title = "고정비", amount = fixedCostsTotal, icon = "🅿️")
            }
        }
    }
}

@Composable
private fun VehicleComparisonCard(
    comparisons: List<VehicleSpendShare>,
    selectedVehicleId: Long?,
    onSelectVehicle: (com.scheduleapp.domain.carbook.model.Vehicle) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🚘 차량별 유지비 비교 (${comparisons.size}대)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            comparisons.forEachIndexed { idx, share ->
                val isHighlighted = selectedVehicleId == share.vehicle.id
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectVehicle(share.vehicle) }
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (idx == 0) "🚗 ${share.vehicle.name}" else "🚙 ${share.vehicle.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${share.vehicle.model})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${formatCurrency(share.amount)} (${share.percentage}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (share.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (idx == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }
                if (idx < comparisons.lastIndex) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun SubStatColumn(title: String, amount: Double, icon: String) {
    Column {
        Text(
            text = "$icon $title",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = formatCurrency(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MonthlyTrendBarChart(
    monthlyTrend: List<MonthlyExpenseBarData>,
    selectedMonth: Int?,
    onMonthClick: (Int) -> Unit
) {
    val maxSpend = (monthlyTrend.maxOfOrNull { it.totalAmount } ?: 0.0).coerceAtLeast(1000.0)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                monthlyTrend.forEach { data ->
                    val isSelected = selectedMonth == data.month
                    val barHeightFraction = (data.totalAmount / maxSpend).toFloat().coerceIn(0.04f, 1f)
                    val barColor = if (isSelected) primaryColor else secondaryColor

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onMonthClick(data.month) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Value label above bar
                        if (data.totalAmount > 0) {
                            Text(
                                text = formatShortAmount(data.totalAmount),
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        // Bar rect
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .fillMaxHeight(barHeightFraction * 0.85f)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barColor)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Month Labels Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            monthlyTrend.forEach { data ->
                val isSelected = selectedMonth == data.month
                Text(
                    text = "${data.month}",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryDonutChart(
    slices: List<ExpenseCategorySlice>,
    totalSpend: Double
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(190.dp)) {
            val strokeWidth = 38.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            var currentAngle = -90f
            for (slice in slices) {
                val sweepAngle = (slice.percentage / 100.0 * 360.0).toFloat()
                drawArc(
                    color = Color(slice.colorHex),
                    startAngle = currentAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                currentAngle += sweepAngle
            }
        }

        // Center Donut Text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "총 지출",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatShortAmount(totalSpend),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CategoryLegendRow(slice: ExpenseCategorySlice) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(slice.colorHex))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${slice.type.icon} ${slice.type.displayName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${slice.percentage}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = formatCurrency(slice.amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val integerPart = amount.toLong().toString()
    val formatted = buildString {
        val len = integerPart.length
        for (i in 0 until len) {
            append(integerPart[i])
            if ((len - 1 - i) % 3 == 0 && i != len - 1) {
                append(',')
            }
        }
    }
    return "$formatted 원"
}

private fun formatShortAmount(amount: Double): String {
    return when {
        amount >= 100_000_000 -> "${round(amount / 100_000_000.0 * 10) / 10}억"
        amount >= 10_000 -> "${round(amount / 10_000.0 * 10) / 10}만"
        else -> "${amount.toLong()}"
    }
}
