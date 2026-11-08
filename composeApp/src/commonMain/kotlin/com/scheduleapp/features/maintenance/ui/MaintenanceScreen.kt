package com.scheduleapp.features.maintenance.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scheduleapp.core.util.DateTimeUtils
import com.scheduleapp.domain.carbook.model.MaintenanceItem
import com.scheduleapp.domain.carbook.model.MaintenanceStatus
import com.scheduleapp.features.maintenance.viewmodel.MaintenanceItemWithWear
import com.scheduleapp.features.maintenance.viewmodel.MaintenanceViewModel

@Composable
fun MaintenanceScreen(
    viewModel: MaintenanceViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var itemToLogService by remember { mutableStateOf<MaintenanceItem?>(null) }
    var itemToDelete by remember { mutableStateOf<MaintenanceItem?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
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

            // Maintenance Status Overview Banner
            MaintenanceSummaryBanner(
                vehicleName = state.vehicle?.name,
                totalCount = state.items.size,
                overdueCount = state.overdueCount,
                warningCount = state.warningCount,
                okCount = state.okCount,
                onAddPresets = { viewModel.seedDefaultPresetsIfEmpty() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🔧", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "등록된 소모품/정비 항목이 없습니다",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "엔진오일, 필터, 브레이크 패드 등\n필수 소모품을 추가해보세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.seedDefaultPresetsIfEmpty() }
                        ) {
                            Text("기본 소모품 5종 한번에 등록")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.items, key = { it.item.id }) { itemWithWear ->
                        MaintenanceItemCard(
                            itemWithWear = itemWithWear,
                            onLogService = { itemToLogService = itemWithWear.item },
                            onDelete = { itemToDelete = itemWithWear.item }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddMaintenanceDialog(
            currentVehicleOdometer = state.vehicle?.currentOdometer ?: 0.0,
            onDismiss = { showAddDialog = false },
            onConfirm = { partName, cycleKm, cycleMonths, lastDate, lastOdo ->
                viewModel.addMaintenanceItem(partName, cycleKm, cycleMonths, lastDate, lastOdo)
            }
        )
    }

    itemToLogService?.let { item ->
        LogMaintenanceDialog(
            item = item,
            currentVehicleOdometer = state.vehicle?.currentOdometer ?: 0.0,
            onDismiss = { itemToLogService = null },
            onConfirm = { serviceDate, serviceOdometer, cost, memo ->
                viewModel.logServiceReplacement(item.id, serviceDate, serviceOdometer, cost, memo)
            }
        )
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("소모품 삭제") },
            text = { Text("'${item.partName}' 소모품 및 연결된 캘린더 일정을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMaintenanceItem(item.id)
                        itemToDelete = null
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
private fun MaintenanceSummaryBanner(
    vehicleName: String?,
    totalCount: Int,
    overdueCount: Int,
    warningCount: Int,
    okCount: Int,
    onAddPresets: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (!vehicleName.isNullOrBlank()) "$vehicleName • 소모품 관리 상태" else "소모품 관리 상태",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (totalCount == 0) {
                    TextButton(onClick = onAddPresets) {
                        Text("기본 소모품 추가", fontSize = 12.sp)
                    }
                } else {
                    Text(
                        text = "총 ${totalCount}개 관리 중",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusPill(label = "정상", count = okCount, color = Color(0xFF2E7D32))
                StatusPill(label = "점검 필요", count = warningCount, color = Color(0xFFE65100))
                StatusPill(label = "교체 초과", count = overdueCount, color = Color(0xFFC62828))
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label $count",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal,
            color = if (count > 0) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MaintenanceItemCard(
    itemWithWear: MaintenanceItemWithWear,
    onLogService: () -> Unit,
    onDelete: () -> Unit
) {
    val item = itemWithWear.item
    val wear = itemWithWear.wearResult

    val statusColor = when (wear.status) {
        MaintenanceStatus.OVERDUE -> Color(0xFFD32F2F)
        MaintenanceStatus.WARNING -> Color(0xFFF57C00)
        MaintenanceStatus.OK -> Color(0xFF388E3C)
    }

    val statusContainerColor = when (wear.status) {
        MaintenanceStatus.OVERDUE -> Color(0xFFFFEBEE)
        MaintenanceStatus.WARNING -> Color(0xFFFFF3E0)
        MaintenanceStatus.OK -> Color(0xFFE8F5E9)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Part Name + Status Chip + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = item.partName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "교체 주기: ${formatCycle(item.cycleKm, item.cycleMonths)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusContainerColor
                    ) {
                        Text(
                            text = "${wear.status.displayName} (${wear.wearPercentage}%)",
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "✕",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { onDelete() }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            val progressFraction = (wear.wearPercentage / 100.0).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics: Remaining Km & Days
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (wear.remainingKm != null) {
                    val kmText = if (wear.remainingKm >= 0) {
                        "남은 주행: ${formatNumber(wear.remainingKm)} km"
                    } else {
                        "초과 주행: +${formatNumber(-wear.remainingKm)} km"
                    }
                    Text(
                        text = kmText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (wear.remainingKm < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(text = "기간 기준 관리", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                val daysText = if (wear.remainingDays >= 0) {
                    "남은 기간: ${wear.remainingDays}일"
                } else {
                    "기한 초과: +${-wear.remainingDays}일"
                }
                Text(
                    text = daysText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (wear.remainingDays < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Recent Service & Next Schedule
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "최근 교체: ${DateTimeUtils.formatDate(DateTimeUtils.localDateFromMillis(item.lastReplacedDate))} (${formatNumber(item.lastReplacedOdometer)} km)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "예정일: ${DateTimeUtils.formatDate(DateTimeUtils.localDateFromMillis(item.nextDueDate))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // [정비 완료] Action Button
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                FilledTonalButton(
                    onClick = onLogService,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("🔧 정비 완료 기록", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatCycle(cycleKm: Double, cycleMonths: Int): String {
    val kmPart = if (cycleKm > 0) "${formatNumber(cycleKm)} km" else null
    val moPart = if (cycleMonths > 0) "${cycleMonths}개월" else null
    return listOfNotNull(kmPart, moPart).joinToString(" 또는 ")
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
