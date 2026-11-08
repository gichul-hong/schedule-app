package com.scheduleapp.core.di

import com.scheduleapp.domain.carbook.usecase.SyncMaintenanceToScheduleUseCase
import com.scheduleapp.features.maintenance.viewmodel.MaintenanceViewModel
import org.koin.dsl.module

val maintenanceModule = module {
    single {
        SyncMaintenanceToScheduleUseCase(
            carBookRepository = get(),
            scheduleRepository = get()
        )
    }

    factory {
        MaintenanceViewModel(
            carBookRepository = get(),
            syncMaintenanceUseCase = get()
        )
    }
}
