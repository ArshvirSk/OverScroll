package com.overscroll.app.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the total Reels scrolled on a specific date.
 */
@Entity(
    tableName = "daily_counts",
    primaryKeys = ["date", "packageName"]
)
data class DailyCount(
    val date: String, // Format: YYYY-MM-DD (ISO-8601)
    val packageName: String,
    val count: Int
)
