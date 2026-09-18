package com.overscroll.app.service

import com.overscroll.app.config.AppTrackerConfig
import com.overscroll.app.data.history.DailyCount
import com.overscroll.app.data.history.DailyCountAggregated
import com.overscroll.app.data.history.DailyCountDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScrollCountRepository @Inject constructor(
    private val dailyCountDao: DailyCountDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // In-memory snapshot of today's counts per package for instant UI updates
    private val _todayCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val todayCounts: StateFlow<Map<String, Int>> = _todayCounts.asStateFlow()

    // Combined total count for today across all apps
    val combinedTodayCount: Flow<Int> = _todayCounts.map { map -> map.values.sum() }

    private val _isTrackingActive = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val isTrackingActive: StateFlow<Map<String, Boolean>> = _isTrackingActive.asStateFlow()

    // Is any tracked app currently active in the foreground
    val isAnyAppActive: Flow<Boolean> = _isTrackingActive.map { map -> map.values.any { it } }

    init {
        // Load persisted counts from Room on startup
        scope.launch {
            val today = LocalDate.now().toString()
            val counts = dailyCountDao.getCountsByDate(today)
            val map = counts.associate { it.packageName to it.count }
            _todayCounts.value = map
        }
    }

    /**
     * Increment the count by 1 for a specific package.
     */
    fun increment(packageName: String) {
        val today = LocalDate.now().toString()
        
        // Optimistic update
        val currentCounts = _todayCounts.value.toMutableMap()
        val currentCount = currentCounts[packageName] ?: 0
        val newCount = currentCount + 1
        currentCounts[packageName] = newCount
        _todayCounts.value = currentCounts

        // Persist
        scope.launch {
            dailyCountDao.insert(DailyCount(date = today, packageName = packageName, count = newCount))
        }
    }

    /**
     * Reset today's count for all apps.
     */
    fun resetTodayAll() {
        val today = LocalDate.now().toString()
        _todayCounts.value = emptyMap()
        scope.launch {
            AppTrackerConfig.SUPPORTED_APPS.forEach { app ->
                dailyCountDao.insert(DailyCount(date = today, packageName = app.packageName, count = 0))
            }
        }
    }

    /**
     * Reset today's count for a specific app.
     */
    fun resetToday(packageName: String) {
        val today = LocalDate.now().toString()
        val currentCounts = _todayCounts.value.toMutableMap()
        currentCounts[packageName] = 0
        _todayCounts.value = currentCounts
        
        scope.launch {
            dailyCountDao.insert(DailyCount(date = today, packageName = packageName, count = 0))
        }
    }

    /**
     * Update whether an app is currently active in the foreground.
     */
    fun setAppActive(packageName: String, isActive: Boolean) {
        val currentActive = _isTrackingActive.value.toMutableMap()
        currentActive[packageName] = isActive
        _isTrackingActive.value = currentActive
    }

    fun getAllHistory(): Flow<List<DailyCount>> {
        return dailyCountDao.getAllHistory()
    }

    fun getLast7DaysCombined(): Flow<List<DailyCountAggregated>> {
        return dailyCountDao.getLast7DaysCombined()
    }

    fun getLast7DaysForPackage(packageName: String): Flow<List<DailyCount>> {
        return dailyCountDao.getLast7DaysForPackage(packageName)
    }
}
