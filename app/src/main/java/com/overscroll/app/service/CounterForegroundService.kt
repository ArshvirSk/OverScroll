package com.overscroll.app.service

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Ongoing status bar notification service (PRD §5.4).
 *
 * Runs as a foreground service with TYPE_SPECIAL_USE.
 * Observes the live count from ScrollCountRepository and updates the notification
 * in real-time.
 *
 * Can be toggled on/off from Settings.
 */
@AndroidEntryPoint
class CounterForegroundService : LifecycleService() {

    @Inject lateinit var scrollCountRepository: ScrollCountRepository

    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val TAG = "CounterService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Counter foreground service created")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        NotificationHelper.createChannels(this)
        
        startAsForeground()
        observeCount()
    }

    private fun startAsForeground() {
        val notification = NotificationHelper.createCounterNotification(this, emptyMap())
        
        ServiceCompat.startForeground(
            this,
            NotificationHelper.NOTIFICATION_ID_COUNTER,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
    }

    private fun observeCount() {
        lifecycleScope.launch {
            scrollCountRepository.todayCounts.collect { countsMap ->
                updateNotification(countsMap)
            }
        }
    }

    private fun updateNotification(countsMap: Map<String, Int>) {
        val notification = NotificationHelper.createCounterNotification(this, countsMap)
        notificationManager.notify(NotificationHelper.NOTIFICATION_ID_COUNTER, notification)
    }

    override fun onDestroy() {
        Log.i(TAG, "Counter foreground service destroyed")
        super.onDestroy()
    }
}
