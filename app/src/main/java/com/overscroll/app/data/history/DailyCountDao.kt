package com.overscroll.app.data.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyCountDao {
    @Query("SELECT * FROM daily_counts ORDER BY date DESC")
    fun getAllHistory(): Flow<List<DailyCount>>

    @Query("SELECT date, SUM(count) as totalCount FROM daily_counts GROUP BY date ORDER BY date DESC LIMIT 7")
    fun getLast7DaysCombined(): Flow<List<DailyCountAggregated>>

    @Query("SELECT * FROM daily_counts WHERE packageName = :packageName ORDER BY date DESC LIMIT 7")
    fun getLast7DaysForPackage(packageName: String): Flow<List<DailyCount>>
    @Query("SELECT date, SUM(count) as totalCount FROM daily_counts GROUP BY date ORDER BY date DESC LIMIT 30")
    fun getLast30DaysCombined(): Flow<List<DailyCountAggregated>>

    @Query("SELECT * FROM daily_counts WHERE packageName = :packageName ORDER BY date DESC LIMIT 30")
    fun getLast30DaysForPackage(packageName: String): Flow<List<DailyCount>>

    @Query("SELECT strftime('%Y-%m', date) as period, SUM(count) as totalCount FROM daily_counts GROUP BY period ORDER BY period DESC LIMIT 12")
    fun getLast12MonthsCombined(): Flow<List<MonthlyCountAggregated>>

    @Query("SELECT strftime('%Y-%m', date) as period, SUM(count) as totalCount FROM daily_counts WHERE packageName = :packageName GROUP BY period ORDER BY period DESC LIMIT 12")
    fun getLast12MonthsForPackage(packageName: String): Flow<List<MonthlyCountAggregated>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dailyCount: DailyCount)

    @Query("SELECT * FROM daily_counts WHERE date = :date AND packageName = :packageName LIMIT 1")
    suspend fun getByDateAndPackage(date: String, packageName: String): DailyCount?

    @Query("SELECT * FROM daily_counts WHERE date = :date")
    suspend fun getCountsByDate(date: String): List<DailyCount>
    
    @Query("DELETE FROM daily_counts")
    suspend fun clearAll()

    @Query("DELETE FROM daily_counts WHERE packageName = :packageName")
    suspend fun clearForPackage(packageName: String)
}

data class DailyCountAggregated(
    val date: String,
    val totalCount: Int
)

data class MonthlyCountAggregated(
    val period: String, // format YYYY-MM
    val totalCount: Int
)
