package com.overscroll.app.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.overscroll.app.data.settings.AppSettingsDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NudgeActionReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var appSettingsDataStore: AppSettingsDataStore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.overscroll.app.ACTION_SNOOZE_NUDGES") {
            // Dismiss the notification
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NotificationHelper.NOTIFICATION_ID_ALERT)
            
            // Set snooze state
            CoroutineScope(Dispatchers.IO).launch {
                appSettingsDataStore.setSnoozedToday(true)
            }
            
            Toast.makeText(context, "Overscroll nudges snoozed for today", Toast.LENGTH_SHORT).show()
        }
    }
}
