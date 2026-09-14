package com.overscroll.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.overscroll.app.data.settings.AppSettingsDataStore
import com.overscroll.app.overlay.OverlayBubbleService
import com.overscroll.app.service.CounterForegroundService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Automatically restarts background monitoring services on device reboot or app update,
 * provided the user had them enabled in settings.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var appSettingsDataStore: AppSettingsDataStore

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && 
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        Log.i(TAG, "Received boot/update broadcast, checking preferences...")
        
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val overlayEnabled = appSettingsDataStore.overlayEnabled.first()
                val notificationEnabled = appSettingsDataStore.notificationEnabled.first()
                
                if (overlayEnabled && Settings.canDrawOverlays(context)) {
                    Log.i(TAG, "Auto-starting OverlayBubbleService")
                    context.startForegroundService(
                        Intent(context, OverlayBubbleService::class.java)
                    )
                }

                if (notificationEnabled) {
                    Log.i(TAG, "Auto-starting CounterForegroundService")
                    context.startForegroundService(
                        Intent(context, CounterForegroundService::class.java)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to auto-start services", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
