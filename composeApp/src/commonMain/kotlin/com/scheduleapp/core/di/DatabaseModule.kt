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
}
