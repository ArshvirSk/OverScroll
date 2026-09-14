package com.overscroll.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.overscroll.app.ui.navigation.AppNavigation
import com.overscroll.app.ui.theme.OverscrollTheme
import dagger.hilt.android.AndroidEntryPoint

import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.lifecycleScope
import com.overscroll.app.data.settings.AppSettingsDataStore
import com.overscroll.app.overlay.OverlayBubbleService
import com.overscroll.app.service.CounterForegroundService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appSettingsDataStore: AppSettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            try {
                val overlayEnabled = appSettingsDataStore.overlayEnabled.first()
                if (overlayEnabled && Settings.canDrawOverlays(this@MainActivity)) {
                    startForegroundService(Intent(this@MainActivity, OverlayBubbleService::class.java))
                }
                
                val notificationEnabled = appSettingsDataStore.notificationEnabled.first()
                if (notificationEnabled) {
                    startForegroundService(Intent(this@MainActivity, CounterForegroundService::class.java))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent {
            OverscrollTheme {
                AppNavigation()
            }
        }
    }
}
