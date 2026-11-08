package com.scheduleapp.features.carbook.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.carbook.model.CarExpenseLog
import com.scheduleapp.domain.carbook.model.FuelRecord
import com.scheduleapp.domain.carbook.model.Vehicle
import com.scheduleapp.features.carbook.viewmodel.CarBookTab
import com.scheduleapp.features.carbook.viewmodel.CarBookViewModel
import com.scheduleapp.features.maintenance.ui.MaintenanceScreen
import com.scheduleapp.features.maintenance.viewmodel.MaintenanceViewModel
import com.scheduleapp.features.reports.ui.ExpenseReportScreen
import com.scheduleapp.features.reports.viewmodel.ExpenseReportViewModel
import org.koin.compose.koinInject

@Composable
fun CarBookScreen(
    viewModel: CarBookViewModel,
    maintenanceViewModel: MaintenanceViewModel = koinInject(),
    expenseReportViewModel: ExpenseReportViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    var showFuelSheet by remember { mutableStateOf(false) }
    var showExpenseSheet by remember { mutableStateOf(false) }
    var showVehicleProfileDialog by remember { mutableStateOf(false) }
    var isAddingNewVehicle by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Synchronize selected vehicle across Maintenance and Expense Report ViewModels
    LaunchedEffect(state.selectedVehicle) {
        state.selectedVehicle?.let { vehicle ->
            maintenanceViewModel.selectVehicle(vehicle)
            expenseReportViewModel.selectVehicle(vehicle)
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    val isFuelOrExpenseTab = state.selectedTab == CarBookTab.FUEL || state.selectedTab == CarBookTab.EXPENSE

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (isFuelOrExpenseTab) {
                Column(horizontalAlignment = Alignment.End) {
                    if (showFabMenu) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                showExpenseSheet = true
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text("💳 기타 지출 추가")
                        }

                        ExtendedFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                showFuelSheet = true
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text("⛽ 주유 기록 추가")
                        }
                    }

                    FloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(
                            text = if (showFabMenu) "✕" else "+",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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

            // Multi-Vehicle Switcher Bar (Always visible across all 4 tabs for 2+ car owners)
            MultiVehicleSwitcherBar(
                vehicles = state.vehicles,
                selectedVehicle = state.selectedVehicle,
                onSelectVehicle = { vehicle ->
                    viewModel.selectVehicle(vehicle)
                    maintenanceViewModel.selectVehicle(vehicle)
                    expenseReportViewModel.selectVehicle(vehicle)
                },
                onAddVehicleClick = {
                    isAddingNewVehicle = true
                    showVehicleProfileDialog = true
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Vehicle Header Card shown on Fuel and Expense tabs
            if (isFuelOrExpenseTab) {
                val vehicle = state.selectedVehicle
                if (vehicle != null) {
                    VehicleHeaderCard(
                        vehicle = vehicle,
                        averageKmPerL = state.averageKmPerL,
                        costPerKm = state.costPerKm,
                        totalFuelCost = state.totalFuelCost,
                        onEditClick = {
                            isAddingNewVehicle = false
                            showVehicleProfileDialog = true
                        }
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Tab Switcher: [주유 기록] · [정비/소모품] · [지출 내역] · [유지비 리포트]
            TabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                CarBookTab.entries.forEach { tab ->
                    val count = when (tab) {
                        CarBookTab.FUEL -> " (${state.fuelRecords.size})"
                        CarBookTab.EXPENSE -> " (${state.expenseLogs.size})"
                        CarBookTab.MAINTENANCE -> ""
                        CarBookTab.REPORT -> ""
                    }
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = {
                            Text(
                                text = "${tab.title}$count",
                                fontWeight = if (state.selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content per Tab
            when (state.selectedTab) {
                CarBookTab.FUEL -> {
                    if (state.fuelRecords.isEmpty()) {
                        EmptyStateView(
                            icon = "⛽",
                            title = "등록된 주유 기록이 없습니다",
                            subtitle = "+ 버튼을 눌러 첫 주유 기록을 입력해보세요."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.fuelRecords, key = { it.expenseId }) { record ->
                                FuelRecordCard(
                                    record = record,
                                    onDelete = { viewModel.deleteFuelRecord(record.expenseId) }
                                )
                            }
                        }
                    }
                }
                CarBookTab.MAINTENANCE -> {
                    MaintenanceScreen(viewModel = maintenanceViewModel)
                }
                CarBookTab.EXPENSE -> {
                    if (state.expenseLogs.isEmpty()) {
                        EmptyStateView(
                            icon = "💳",
                            title = "등록된 지출 내역이 없습니다",
                            subtitle = "+ 버튼을 눌러 정비, 주차, 톨게이트 등 지출을 입력해보세요."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.expenseLogs, key = { it.id }) { expense ->
                                ExpenseLogCard(
                                    expense = expense,
                                    onDelete = { viewModel.deleteExpense(expense) }
                                )
                            }
                        }
                    }
                }
                CarBookTab.REPORT -> {
                    ExpenseReportScreen(viewModel = expenseReportViewModel)
                }
            }
        }
    }

    // Dialogs & Sheets
    if (showFuelSheet) {
        AddFuelBottomSheet(
            currentVehicleOdometer = state.selectedVehicle?.currentOdometer ?: 0.0,
            onDismiss = { showFuelSheet = false },
            onConfirm = { amount, liters, unitPrice, odometer, isFullTank, date, memo ->
                viewModel.addFuelRecord(amount, liters, unitPrice, odometer, isFullTank, date, memo)
            }
        )
    }

    if (showExpenseSheet) {
        AddExpenseBottomSheet(
            currentVehicleOdometer = state.selectedVehicle?.currentOdometer ?: 0.0,
            onDismiss = { showExpenseSheet = false },
            onConfirm = { type, amount, odometer, date, memo ->
                viewModel.addExpense(type, amount, odometer, date, memo)
            }
        )
    }

    if (showVehicleProfileDialog) {
        VehicleProfileDialog(
            initialVehicle = if (isAddingNewVehicle) null else state.selectedVehicle,
            canDelete = !isAddingNewVehicle && state.vehicles.size > 1,
            onDismiss = { showVehicleProfileDialog = false },
            onSave = { updated ->
                viewModel.saveVehicleProfile(updated)
            },
            onDelete = { toDelete ->
                viewModel.deleteVehicle(toDelete)
            }
        )
    }
}

@Composable
private fun MultiVehicleSwitcherBar(
    vehicles: List<Vehicle>,
    selectedVehicle: Vehicle?,
    onSelectVehicle: (Vehicle) -> Unit,
    onAddVehicleClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        vehicles.forEachIndexed { index, vehicle ->
            val isSelected = selectedVehicle?.id == vehicle.id
            val carIcon = if (index == 0) "🚗" else "🚙"
            FilterChip(
                selected = isSelected,
                onClick = { onSelectVehicle(vehicle) },
                label = {
                    Text(
                        text = "$carIcon ${vehicle.name} (${vehicle.model})",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            )
        }

        AssistChip(
            onClick = onAddVehicleClick,
            label = {
                Text(
                    text = "+ 차량 추가",
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        )
    }
}

@Composable
private fun VehicleHeaderCard(
    vehicle: Vehicle,
    averageKmPerL: Double?,
    costPerKm: Double?,
    totalFuelCost: Double,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Vehicle Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🚗 ${vehicle.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    SuggestionChip(
                        onClick = {},
                        label = { Text(vehicle.model, style = MaterialTheme.typography.labelSmall) }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text(vehicle.fuelType.displayName, style = MaterialTheme.typography.labelSmall) }
                    )
                }

                TextButton(onClick = onEditClick) {
                    Text("관리", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Current Odometer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "현재 주행거리: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${formatNumber(vehicle.currentOdometer)} km",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Statistics Banner (Full-to-Full Fuel Economy & Cost per km)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatMetric(
                    label = "평균 연비",
                    value = if (averageKmPerL != null) "$averageKmPerL km/L" else "-"
                )
                StatMetric(
                    label = "1km 주행비",
                    value = if (costPerKm != null) "$costPerKm 원/km" else "-"
                )
                StatMetric(
                    label = "누적 주유비",
                    value = formatCurrency(totalFuelCost)
                )
            }
        }
    }
}

@Composable
private fun StatMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FuelRecordCard(
    record: FuelRecord,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Date & Odometer & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = DateTimeUtils.formatDate(DateTimeUtils.localDateFromMillis(record.date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  ${formatNumber(record.odometer)} km",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "삭제",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onDelete() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Row: Amount & Liters & Unit Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = formatCurrency(record.amount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${record.liters} L (${formatNumber(record.unitPrice)} 원/L)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Badges: Full-tank & Fuel Efficiency
                Column(horizontalAlignment = Alignment.End) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (record.isFullTank) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = if (record.isFullTank) "가득" else "부분",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (record.calculatedEfficiency != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "${record.calculatedEfficiency} km/L",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    if (record.costPerKm != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${record.costPerKm} 원/km",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!record.memo.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "메모: ${record.memo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExpenseLogCard(
    expense: CarExpenseLog,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = expense.type.icon, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = expense.type.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = DateTimeUtils.formatDate(DateTimeUtils.localDateFromMillis(expense.date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!expense.memo.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = expense.memo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (expense.odometer > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${formatNumber(expense.odometer)} km",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Amount and Delete
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(expense.amount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "삭제",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onDelete() }
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    icon: String,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = icon, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
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

private fun formatNumber(number: Double): String {
    val integerPart = number.toLong().toString()
    val formatted = buildString {
        val len = integerPart.length
        for (i in 0 until len) {
            append(integerPart[i])
            if ((len - 1 - i) % 3 == 0 && i != len - 1) {
                append(',')
            }
        }
    }
    return formatted
}
