package com.scheduleapp.core.di

import com.scheduleapp.core.database.AppDatabase
import com.scheduleapp.core.database.createRoomDatabase
import com.scheduleapp.core.database.getDatabaseBuilder
import org.koin.dsl.module

val databaseModule = module {
    single<AppDatabase> {
        createRoomDatabase(getDatabaseBuilder())
    }
    single { get<AppDatabase>().scheduleDao() }
    single { get<AppDatabase>().carBookDao() }
    single { com.scheduleapp.core.platform.DatabaseBackupEngine(get(), get()) }
    single { com.scheduleapp.core.platform.PlatformCalendarSync() }
    single { com.scheduleapp.core.platform.PlatformNotificationScheduler() }
    single { com.scheduleapp.core.platform.PlatformHttpEngine() }
    single { com.scheduleapp.core.platform.PlatformBase64() }
    single { com.scheduleapp.core.platform.PlatformFileStorage() }
    single { com.scheduleapp.core.platform.GitHubSyncEngine(get(), get(), get(), get(), get()) }
}
