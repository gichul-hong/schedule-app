package com.scheduleapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.scheduleapp.core.di.*
import com.scheduleapp.core.theme.ScheduleAppTheme
import com.scheduleapp.features.calendar.ui.CalendarScreen
import com.scheduleapp.features.calendar.viewmodel.CalendarViewModel
import com.scheduleapp.features.carbook.ui.CarBookScreen
import com.scheduleapp.features.carbook.viewmodel.CarBookViewModel
import com.scheduleapp.features.routine.ui.RoutineScreen
import com.scheduleapp.features.routine.viewmodel.RoutineViewModel
import com.scheduleapp.features.settings.SettingsScreen
import com.scheduleapp.features.settings.SettingsViewModel
import com.scheduleapp.features.timeline.ui.IntegratedTimelineScreen
import com.scheduleapp.features.timeline.viewmodel.TimelineViewModel
import com.scheduleapp.features.todo.ui.TodoScreen
import com.scheduleapp.features.todo.viewmodel.TodoViewModel
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

enum class AppTab(val title: String, val icon: String) {
    SCHEDULE("캘린더/할일", "📅"),
    CARBOOK("차계부", "🚗"),
    ROUTINE("루틴", "🔥"),
    TIMELINE("타임라인", "⏱"),
    SETTINGS("설정", "⚙️")
}

@Composable
fun App() {
    KoinApplication(application = {
        modules(
            databaseModule,
            scheduleModule,
            carBookModule,
            maintenanceModule,
            reportModule,
            settingsModule
        )
    }) {
        val settingsViewModel: SettingsViewModel = koinInject()
        val settingsState by settingsViewModel.uiState.collectAsState()

        ScheduleAppTheme(themeMode = settingsState.themeMode) {
            AppContent(settingsViewModel = settingsViewModel)
        }
    }
}

@Composable
fun AppContent(
    calendarViewModel: CalendarViewModel = koinInject(),
    todoViewModel: TodoViewModel = koinInject(),
    routineViewModel: RoutineViewModel = koinInject(),
    timelineViewModel: TimelineViewModel = koinInject(),
    carBookViewModel: CarBookViewModel = koinInject(),
    settingsViewModel: SettingsViewModel = koinInject()
) {
    var selectedTab by remember { mutableStateOf(AppTab.SCHEDULE) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.icon, fontSize = 20.sp) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                AppTab.SCHEDULE -> ScheduleAndTodoScreen(
                    calendarViewModel = calendarViewModel,
                    todoViewModel = todoViewModel
                )
                AppTab.CARBOOK -> CarBookScreen(viewModel = carBookViewModel)
                AppTab.ROUTINE -> RoutineScreen(viewModel = routineViewModel)
                AppTab.TIMELINE -> IntegratedTimelineScreen(viewModel = timelineViewModel)
                AppTab.SETTINGS -> SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}

@Composable
fun ScheduleAndTodoScreen(
    calendarViewModel: CalendarViewModel,
    todoViewModel: TodoViewModel
) {
    var selectedSubTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = {
                    Text(
                        "캘린더 일정",
                        fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = {
                    Text(
                        "할 일 (To-Do)",
                        fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedSubTab) {
                0 -> CalendarScreen(viewModel = calendarViewModel)
                1 -> TodoScreen(viewModel = todoViewModel)
            }
        }
    }
}
