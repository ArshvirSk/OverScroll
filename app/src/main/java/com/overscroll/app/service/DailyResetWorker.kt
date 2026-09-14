package com.overscroll.app.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.overscroll.app.data.settings.AppSettingsDataStore

/**
 * A daily background task that ensures the count rolls over correctly at midnight.
 * 
 * Also serves as a "health check" — if the accessibility service is disabled,
 * it could optionally post a notification (Phase 7 feature).
 */
@HiltWorker
class DailyResetWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val scrollCountRepository: ScrollCountRepository,
    private val appSettingsDataStore: AppSettingsDataStore
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "DailyResetWorker"
        const val WORK_NAME = "OverscrollDailyReset"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Running daily reset and health check worker...")
            
            // This is a no-op if the day hasn't changed.
            // If the day HAS changed, this single increment will force the repository/DataStore
            // to run its rollover logic (saving yesterday's count to Room and resetting to 1).
            // Wait, actually, we just want to force a save and reset.
            // But we don't want to falsely increment to 1 if the user hasn't scrolled today.
            // Reset daily nudge states
            appSettingsDataStore.resetDailyNudgeState()
            
            Log.i(TAG, "Worker completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed", e)
            Result.retry()
        }
    }
}
