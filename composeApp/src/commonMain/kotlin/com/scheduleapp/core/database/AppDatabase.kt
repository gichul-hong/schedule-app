package com.scheduleapp.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.scheduleapp.core.database.dao.CarBookDao
import com.scheduleapp.core.database.dao.ScheduleDao
import com.scheduleapp.core.database.entity.CarExpenseLogEntity
import com.scheduleapp.core.database.entity.FuelDetailEntity
import com.scheduleapp.core.database.entity.MaintenanceItemEntity
import com.scheduleapp.core.database.entity.RoutineEntity
import com.scheduleapp.core.database.entity.RoutineLogEntity
import com.scheduleapp.core.database.entity.ScheduleEventEntity
import com.scheduleapp.core.database.entity.TodoItemEntity
import com.scheduleapp.core.database.entity.VehicleEntity

@Database(
    entities = [
        ScheduleEventEntity::class,
        TodoItemEntity::class,
        RoutineEntity::class,
        RoutineLogEntity::class,
        VehicleEntity::class,
        CarExpenseLogEntity::class,
        FuelDetailEntity::class,
        MaintenanceItemEntity::class
    ],
    version = 2,
    exportSchema = true
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun carBookDao(): CarBookDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>
