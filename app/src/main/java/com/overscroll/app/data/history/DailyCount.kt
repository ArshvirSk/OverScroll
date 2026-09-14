package com.overscroll.app.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the total Reels scrolled on a specific date.
 */
@Entity(tableName = "daily_counts")
data class DailyCount(
    @PrimaryKey
    val date: String, // Format: YYYY-MM-DD (ISO-8601)
    val count: Int
)
