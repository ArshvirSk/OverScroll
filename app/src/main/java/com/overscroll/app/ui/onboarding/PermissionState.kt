package com.overscroll.app.ui.onboarding

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import com.overscroll.app.service.ReelsAccessibilityService

/**
 * Live permission status for the three permissions Overscroll needs.
 *
 * - accessibilityEnabled: Is our AccessibilityService registered and running?
 * - overlayEnabled: Can we draw overlays (TYPE_APPLICATION_OVERLAY)?
 * - notificationEnabled: Can we post notifications (POST_NOTIFICATIONS on API 33+)?
 *
 * Call [checkPermissions] to snapshot the current state. The onboarding screen
 * re-checks on every Activity resume so the UI updates live when the user
 * returns from system settings.
 */
data class PermissionStatus(
    val accessibilityEnabled: Boolean,
    val overlayEnabled: Boolean,
    val notificationEnabled: Boolean,
) {
    /** True when all required permissions are granted */
    val allGranted: Boolean
        get() = accessibilityEnabled && overlayEnabled && notificationEnabled

    /** Count of granted permissions (for progress indicators) */
    val grantedCount: Int
        get() = listOf(accessibilityEnabled, overlayEnabled, notificationEnabled).count { it }
}

/**
 * Snapshot the current permission state. Safe to call from any thread.
 */
fun checkPermissions(context: Context): PermissionStatus {
    return PermissionStatus(
        accessibilityEnabled = isAccessibilityServiceEnabled(context),
        overlayEnabled = Settings.canDrawOverlays(context),
        notificationEnabled = areNotificationsEnabled(context),
    )
}

/**
 * Check if our AccessibilityService is enabled in system settings.
 *
 * Note: This checks the system's list of *enabled* accessibility services,
 * not whether the service is currently *running*. The system will start
 * the service automatically after it's enabled.
 */
private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        ?: return false
    val enabledServices = am.getEnabledAccessibilityServiceList(
        AccessibilityServiceInfo.FEEDBACK_ALL_MASK
    )
    val targetName = ReelsAccessibilityService::class.java.name
    return enabledServices.any { serviceInfo ->
        serviceInfo.resolveInfo?.serviceInfo?.let { info ->
            info.packageName == context.packageName && info.name == targetName
        } ?: false
    }
}

/**
 * Check if notifications are enabled.
 * On API 33+ this requires POST_NOTIFICATIONS runtime permission.
 * On API < 33, notifications are enabled by default.
 */
private fun areNotificationsEnabled(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    } else {
        true
    }
}
