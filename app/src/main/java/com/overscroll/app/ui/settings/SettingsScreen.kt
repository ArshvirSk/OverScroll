package com.overscroll.app.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overscroll.app.data.settings.AppSettingsDataStore
import com.overscroll.app.overlay.OverlayBubbleService
import com.overscroll.app.service.CounterForegroundService
import com.overscroll.app.service.ScrollCountRepository
import com.overscroll.app.ui.theme.OverscrollError
import com.overscroll.app.ui.theme.OverscrollPrimary
import com.overscroll.app.ui.theme.OverscrollBackgroundTop
import com.overscroll.app.ui.theme.OverscrollBackgroundBottom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore,
    private val scrollCountRepository: ScrollCountRepository,
) : ViewModel() {
    val overlayEnabled = appSettingsDataStore.overlayEnabled
    val notificationEnabled = appSettingsDataStore.notificationEnabled
    val thresholdA = appSettingsDataStore.thresholdA
    val thresholdB = appSettingsDataStore.thresholdB
    val nudgeEnabled = appSettingsDataStore.nudgeEnabled
    val snoozedToday = appSettingsDataStore.snoozedToday

    fun setOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.setOverlayEnabled(enabled)
        }
    }

    fun setNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.setNotificationEnabled(enabled)
        }
    }

    fun setThresholdA(value: Int) {
        viewModelScope.launch {
            // Keep thresholdA strictly less than thresholdB
            val currentB = appSettingsDataStore.thresholdB.first()
            appSettingsDataStore.setThresholds(value.coerceAtMost(currentB - 1), currentB)
        }
    }

    fun setThresholdB(value: Int) {
        viewModelScope.launch {
            // Keep thresholdB strictly greater than thresholdA
            val currentA = appSettingsDataStore.thresholdA.first()
            appSettingsDataStore.setThresholds(currentA, value.coerceAtLeast(currentA + 1))
        }
    }

    fun setNudgeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.setNudgeEnabled(enabled)
        }
    }
    
    fun resetSnooze() {
        viewModelScope.launch {
            appSettingsDataStore.setSnoozedToday(false)
        }
    }

    fun resetTodayCount() {
        scrollCountRepository.resetToday()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val overlayEnabled by viewModel.overlayEnabled.collectAsState(initial = false)
    val notificationEnabled by viewModel.notificationEnabled.collectAsState(initial = false)
    val thresholdA by viewModel.thresholdA.collectAsState(initial = 20)
    val thresholdB by viewModel.thresholdB.collectAsState(initial = 50)
    val nudgeEnabled by viewModel.nudgeEnabled.collectAsState(initial = true)
    val snoozedToday by viewModel.snoozedToday.collectAsState(initial = false)

    // Notification permission launcher (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.setNotificationEnabled(true)
                startCounterService(context)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                )
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OverscrollBackgroundTop,
                        OverscrollBackgroundBottom,
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // -- Display Section --
            Text(
                text = "DISPLAY",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingToggleRow(
                        title = "Floating Bubble",
                        subtitle = "Show live count overlay on screen",
                        checked = overlayEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (Settings.canDrawOverlays(context)) {
                                    viewModel.setOverlayEnabled(true)
                                    startOverlayService(context)
                                } else {
                                    // Request overlay permission
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            } else {
                                viewModel.setOverlayEnabled(false)
                                stopOverlayService(context)
                            }
                        }
                    )
                    
                    SettingToggleRow(
                        title = "Status Bar Counter",
                        subtitle = "Ongoing notification with today's count",
                        checked = notificationEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setNotificationEnabled(true)
                                    startCounterService(context)
                                }
                            } else {
                                viewModel.setNotificationEnabled(false)
                                stopCounterService(context)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // -- Nudges & Expressions Section --
            Text(
                text = "NUDGES & EXPRESSIONS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingToggleRow(
                        title = "Nudge Notifications",
                        subtitle = "Alert me when I cross a threshold",
                        checked = nudgeEnabled,
                        onCheckedChange = { viewModel.setNudgeEnabled(it) }
                    )
                    
                    if (snoozedToday) {
                        Button(
                            onClick = { viewModel.resetSnooze() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text("Un-snooze Nudges for Today")
                        }
                    }

                    // Threshold A Slider
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tired Threshold", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("$thresholdA Reels", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("When the mascot gets tired", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = thresholdA.toFloat(),
                            onValueChange = { viewModel.setThresholdA(it.toInt()) },
                            valueRange = 1f..199f,
                            steps = 198,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Threshold B Slider
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Worn Out Threshold", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("$thresholdB Reels", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("When the mascot gives up", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = thresholdB.toFloat(),
                            onValueChange = { viewModel.setThresholdB(it.toInt()) },
                            valueRange = 2f..300f,
                            steps = 298,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // -- Data Section --
            Text(
                text = "DATA",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { viewModel.resetTodayCount() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = OverscrollError
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null)
                        Text(
                            text = "Reset Today's Count",
                            modifier = Modifier.padding(start = 16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            // App info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Overscroll is fully offline and privacy-focused.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// Helpers to start/stop services
private fun startOverlayService(context: Context) {
    if (Settings.canDrawOverlays(context)) {
        val intent = Intent(context, OverlayBubbleService::class.java)
        context.startForegroundService(intent)
    }
}

private fun stopOverlayService(context: Context) {
    context.stopService(Intent(context, OverlayBubbleService::class.java))
}

private fun startCounterService(context: Context) {
    val intent = Intent(context, CounterForegroundService::class.java)
    context.startForegroundService(intent)
}

private fun stopCounterService(context: Context) {
    context.stopService(Intent(context, CounterForegroundService::class.java))
}
