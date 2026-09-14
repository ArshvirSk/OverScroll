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

    @Query("SELECT * FROM daily_counts ORDER BY date DESC LIMIT 7")
    fun getLast7Days(): Flow<List<DailyCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dailyCount: DailyCount)

    @Query("SELECT * FROM daily_counts WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DailyCount?
    
    @Query("DELETE FROM daily_counts")
    suspend fun clearAll()
}
