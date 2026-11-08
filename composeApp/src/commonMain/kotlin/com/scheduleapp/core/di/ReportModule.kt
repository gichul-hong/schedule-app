package com.scheduleapp.core.di

import com.scheduleapp.features.reports.viewmodel.ExpenseReportViewModel
import org.koin.dsl.module

val reportModule = module {
    factory {
        ExpenseReportViewModel(
            carBookRepository = get()
        )
    }
}
