package com.overscroll.app.service

import com.overscroll.app.data.history.DailyCount
import com.overscroll.app.data.history.DailyCountDao
import com.overscroll.app.data.settings.CounterDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central repository for the Reels scroll count.
 *
 * Architecture:
 * - DataStore (CounterDataStore) is the durable source of truth
 * - StateFlow (_todayCount) is the in-memory snapshot for instant UI updates
 * - On increment: optimistically update StateFlow, then persist to DataStore
 * - On init: load persisted count from DataStore into StateFlow
 *
 * Consumers:
 * - HomeScreen (observes todayCount for the big number display)
 * - OverlayBubbleService (Phase 3: observes for the floating counter)
 * - CounterForegroundService (Phase 4: observes for the notification)
 * - ReelsAccessibilityService (calls increment() on each detected scroll)
 */
@Singleton
class ScrollCountRepository @Inject constructor(
    private val counterDataStore: CounterDataStore,
    private val dailyCountDao: DailyCountDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _todayCount = MutableStateFlow(0)
    val todayCount: StateFlow<Int> = _todayCount.asStateFlow()

    private val _isInstagramActive = MutableStateFlow(false)
    val isInstagramActive: StateFlow<Boolean> = _isInstagramActive.asStateFlow()

    init {
        // Load persisted count from DataStore on startup.
        // The Flow will also emit when the day rolls over (count → 0).
        scope.launch {
            counterDataStore.todayCount.collect { persistedCount ->
                _todayCount.value = persistedCount
            }
        }
    }

    /**
     * Increment the count by 1.
     * Updates the in-memory StateFlow immediately (optimistic) for snappy UI,
     * then persists to DataStore in the background.
     */
    fun increment() {
        _todayCount.value++
        scope.launch {
            counterDataStore.incrementCount()
        }
    }

    /**
     * Reset today's count to 0 (manual reset from Settings).
     */
    fun resetToday() {
        _todayCount.value = 0
        scope.launch {
            counterDataStore.resetTodayCount()
        }
    }

    /**
     * Update whether Instagram is currently the active foreground app.
     */
    fun setInstagramActive(isActive: Boolean) {
        _isInstagramActive.value = isActive
    }

    /**
     * Set count to a specific value.
     */
    fun setCount(count: Int) {
        _todayCount.value = count
        scope.launch {
            counterDataStore.setCount(count)
        }
    }

    /**
     * History flows from Room.
     */
    fun getAllHistory(): Flow<List<DailyCount>> {
        return dailyCountDao.getAllHistory()
    }

    fun getLast7Days(): Flow<List<DailyCount>> {
        return dailyCountDao.getLast7Days()
    }
}
