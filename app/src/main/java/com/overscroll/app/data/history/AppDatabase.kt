package com.overscroll.app.data.history

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DailyCount::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyCountDao(): DailyCountDao
}
