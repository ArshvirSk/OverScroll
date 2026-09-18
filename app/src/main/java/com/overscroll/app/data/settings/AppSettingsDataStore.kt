package com.overscroll.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

import androidx.datastore.preferences.core.intPreferencesKey

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * DataStore for user preferences and state.
 */
@Singleton
class AppSettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        private val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        
        // v1.2 Nudge & Threshold settings
        private val KEY_THRESHOLD_A = intPreferencesKey("threshold_a")
        private val KEY_THRESHOLD_B = intPreferencesKey("threshold_b")
        private val KEY_NUDGE_ENABLED = booleanPreferencesKey("nudge_enabled")
        
        // v1.2 Daily Nudge state
        private val KEY_SNOOZED_TODAY = booleanPreferencesKey("snoozed_today")
        private val KEY_NUDGED_A_TODAY = booleanPreferencesKey("nudged_a_today")
        private val KEY_NUDGED_B_TODAY = booleanPreferencesKey("nudged_b_today")
        
        // v2 Tracked Apps
        private val KEY_TRACKED_APPS = androidx.datastore.preferences.core.stringSetPreferencesKey("tracked_apps")
    }

    val trackedApps: Flow<Set<String>> = context.appSettingsDataStore.data.map { prefs ->
        prefs[KEY_TRACKED_APPS] ?: setOf("com.instagram.android") // Default to IG only for backwards compat
    }

    val overlayEnabled: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[KEY_OVERLAY_ENABLED] ?: true
    }

    val notificationEnabled: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATION_ENABLED] ?: false
    }

    val thresholdA: Flow<Int> = context.appSettingsDataStore.data.map { prefs ->
        prefs[KEY_THRESHOLD_A] ?: 20
    }

    val thresholdB: Flow<Int> = context.appSettingsDataStore.data.map { prefs ->
        prefs[KEY_THRESHOLD_B] ?: 50
    }

    val nudgeEnabled: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[KEY_NUDGE_ENABLED] ?: true
    }

    val snoozedToday: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[KEY_SNOOZED_TODAY] ?: false
    }

    val nudgedAToday: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[KEY_NUDGED_A_TODAY] ?: false
    }

    val nudgedBToday: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[KEY_NUDGED_B_TODAY] ?: false
    }

    suspend fun setTrackedApps(apps: Set<String>) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_TRACKED_APPS] = apps
        }
    }

    suspend fun toggleTrackedApp(packageName: String, enabled: Boolean) {
        context.appSettingsDataStore.edit { prefs ->
            val current = prefs[KEY_TRACKED_APPS]?.toMutableSet() ?: mutableSetOf("com.instagram.android")
            if (enabled) {
                current.add(packageName)
            } else {
                current.remove(packageName)
            }
            prefs[KEY_TRACKED_APPS] = current
        }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_OVERLAY_ENABLED] = enabled
        }
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_NOTIFICATION_ENABLED] = enabled
        }
    }

    suspend fun setThresholds(a: Int, b: Int) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_THRESHOLD_A] = a
            prefs[KEY_THRESHOLD_B] = b
        }
    }

    suspend fun setNudgeEnabled(enabled: Boolean) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_NUDGE_ENABLED] = enabled
        }
    }

    suspend fun setSnoozedToday(snoozed: Boolean) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_SNOOZED_TODAY] = snoozed
        }
    }

    suspend fun setNudgedAToday(nudged: Boolean) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_NUDGED_A_TODAY] = nudged
        }
    }

    suspend fun setNudgedBToday(nudged: Boolean) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_NUDGED_B_TODAY] = nudged
        }
    }

    suspend fun resetDailyNudgeState() {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_SNOOZED_TODAY] = false
            prefs[KEY_NUDGED_A_TODAY] = false
            prefs[KEY_NUDGED_B_TODAY] = false
        }
    }
}
