package com.overscroll.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import com.overscroll.app.data.history.DailyCount
import com.overscroll.app.data.history.DailyCountDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore wrapper for the running Reels counter.
 *
 * Stores today's count and the date it belongs to. When the date rolls over,
 * the count resets to 0 (the previous day's count will be saved to Room in Phase 5).
 *
 * This is the persistence layer for ScrollCountRepository. The StateFlow in that
 * repository provides the in-memory snapshot for instant UI updates; DataStore
 * is the durable source of truth.
 */

// Single DataStore instance per process — must be at file level per Jetpack docs
private val Context.counterDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "counter"
)

@Singleton
class CounterDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dailyCountDao: DailyCountDao,
) {
    companion object {
        private val KEY_TODAY_COUNT = intPreferencesKey("today_count")
        private val KEY_TODAY_DATE = stringPreferencesKey("today_date")
    }

    /**
     * Flow of today's count. Automatically resets to 0 if the stored date
     * doesn't match the current date (i.e., a new day has started).
     */
    val todayCount: Flow<Int> = context.counterDataStore.data.map { prefs ->
        val savedDate = prefs[KEY_TODAY_DATE] ?: ""
        val today = LocalDate.now().toString()
        if (savedDate == today) {
            prefs[KEY_TODAY_COUNT] ?: 0
        } else {
            0 // New day — count hasn't been written yet
        }
    }

    /**
     * Flow of the raw stored date string, for diagnostics/debugging.
     */
    val storedDate: Flow<String> = context.counterDataStore.data.map { prefs ->
        prefs[KEY_TODAY_DATE] ?: ""
    }

    /**
     * Atomically increment today's count by 1.
     * If the stored date doesn't match today, resets to 1 (first Reel of the new day).
     * DataStore edits are serialized, so concurrent calls are safe.
     */
    suspend fun incrementCount() {
        context.counterDataStore.edit { prefs ->
            val today = LocalDate.now().toString()
            val savedDate = prefs[KEY_TODAY_DATE] ?: ""
            if (savedDate != today) {
                // New day: save yesterday's final count to Room if it exists
                if (savedDate.isNotEmpty()) {
                    val lastCount = prefs[KEY_TODAY_COUNT] ?: 0
                    dailyCountDao.insert(DailyCount(date = savedDate, count = lastCount))
                }
                // Reset for today
                prefs[KEY_TODAY_DATE] = today
                prefs[KEY_TODAY_COUNT] = 1
            } else {
                prefs[KEY_TODAY_COUNT] = (prefs[KEY_TODAY_COUNT] ?: 0) + 1
            }
        }
    }

    /**
     * Reset today's count to 0 (manual reset from Settings).
     */
    suspend fun resetTodayCount() {
        context.counterDataStore.edit { prefs ->
            prefs[KEY_TODAY_COUNT] = 0
            prefs[KEY_TODAY_DATE] = LocalDate.now().toString()
        }
    }

    /**
     * Set count to a specific value (used for testing/migration).
     */
    suspend fun setCount(count: Int) {
        context.counterDataStore.edit { prefs ->
            prefs[KEY_TODAY_COUNT] = count
            prefs[KEY_TODAY_DATE] = LocalDate.now().toString()
        }
    }
}
