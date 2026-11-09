package com.scheduleapp.core.di

import com.scheduleapp.features.settings.SettingsViewModel
import org.koin.dsl.module

val settingsModule = module {
    factory { SettingsViewModel(get(), get(), get()) }
}
