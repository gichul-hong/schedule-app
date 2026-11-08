package com.scheduleapp.core.di

import com.scheduleapp.domain.calendar.repository.ScheduleRepository
import com.scheduleapp.domain.calendar.repository.ScheduleRepositoryImpl
import com.scheduleapp.features.calendar.viewmodel.CalendarViewModel
import com.scheduleapp.features.routine.viewmodel.RoutineViewModel
import com.scheduleapp.features.timeline.viewmodel.TimelineViewModel
import com.scheduleapp.features.todo.viewmodel.TodoViewModel
import org.koin.dsl.module

val scheduleModule = module {
    single<ScheduleRepository> {
        ScheduleRepositoryImpl(scheduleDao = get())
    }

    factory { CalendarViewModel(repository = get()) }
    factory { TodoViewModel(repository = get()) }
    factory { RoutineViewModel(repository = get()) }
    factory { TimelineViewModel(scheduleRepository = get(), carBookDao = get()) }
}
