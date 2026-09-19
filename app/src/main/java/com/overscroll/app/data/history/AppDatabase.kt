package com.overscroll.app.data.history

import androidx.room.Database
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [DailyCount::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyCountDao(): DailyCountDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the new table
                database.execSQL(
                    "CREATE TABLE daily_counts_new (" +
                            "date TEXT NOT NULL, " +
                            "packageName TEXT NOT NULL, " +
                            "count INTEGER NOT NULL, " +
                            "PRIMARY KEY(date, packageName))"
                )
                // Copy the data (defaulting packageName to Instagram for existing records)
                database.execSQL(
                    "INSERT INTO daily_counts_new (date, packageName, count) " +
                            "SELECT date, 'com.instagram.android', count FROM daily_counts"
                )
                // Remove the old table
                database.execSQL("DROP TABLE daily_counts")
                // Change the table name to the correct one
                database.execSQL("ALTER TABLE daily_counts_new RENAME TO daily_counts")
            }
        }
        
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_counts ADD COLUMN timeSpentMs INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
