package com.scheduleapp.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File
import java.io.FileOutputStream

private var appContext: Context? = null

fun initDatabaseContext(context: Context) {
    val appCtx = context.applicationContext
    appContext = appCtx
    seedReceiptAssetsIfNeeded(appCtx)
}

private fun seedReceiptAssetsIfNeeded(context: Context) {
    runCatching {
        val receiptsDir = File(context.filesDir, "receipts").apply { mkdirs() }
        listOf("receipt_20230922.jpg", "receipt_20240223.png").forEach { fileName ->
            val target = File(receiptsDir, fileName)
            if (!target.exists() || target.length() == 0L) {
                context.assets.open("receipts/$fileName").use { input ->
                    FileOutputStream(target).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
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
