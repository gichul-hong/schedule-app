package com.scheduleapp.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

private var appContext: Context? = null

fun initDatabaseContext(context: Context) {
    appContext = context.applicationContext
}

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val context = checkNotNull(appContext) {
        "Database context not initialized. Call initDatabaseContext(context) first."
    }
    val dbFile = context.getDatabasePath("schedule_app.db")
    return Room.databaseBuilder<AppDatabase>(
        context = context,
        name = dbFile.absolutePath
    )
}
