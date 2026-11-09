package com.scheduleapp.core.di

import com.scheduleapp.domain.carbook.repository.CarBookRepository
import com.scheduleapp.domain.carbook.repository.CarBookRepositoryImpl
import com.scheduleapp.features.carbook.viewmodel.CarBookViewModel
import org.koin.dsl.module

val carBookModule = module {
    single<CarBookRepository> {
        CarBookRepositoryImpl(carBookDao = get())
    }

    factory { CarBookViewModel(carBookRepository = get(), syncMaintenanceUseCase = get()) }
}
