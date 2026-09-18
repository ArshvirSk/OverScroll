package com.overscroll.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.overscroll.app.config.AppTrackerConfig
import com.overscroll.app.config.TrackedApp
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Accessibility service generalized for short-form video scroll detection.
 */
class ReelsAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ReelsA11yService"
    }

    /** Timestamp of the last counted scroll per package, for debouncing */
    private val lastScrollTimestamps = mutableMapOf<String, Long>()

    /** Whether we believe the user is currently in the short-form feed per package */
    private val isInFeedViewer = mutableMapOf<String, Boolean>()
    
    /** Timestamp of when we last entered the feed per package */
    private val windowEnterTimestamps = mutableMapOf<String, Long>()

    /** Set of resource IDs we've seen but didn't match — logged for ID discovery */
    private val unknownScrollSources = mutableMapOf<String, MutableSet<String>>()

    /** Enabled packages */
    private var trackedPackages = setOf<String>()

    private val repository: ScrollCountRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            ReelsAccessibilityEntryPoint::class.java,
        ).scrollCountRepository()
    }

    private val appSettings: com.overscroll.app.data.settings.AppSettingsDataStore by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            ReelsAccessibilityEntryPoint::class.java,
        ).appSettingsDataStore()
    }

    // Coroutine scope for observing flows
    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)

    // Cached settings states
    private var currentThresholdA = 20
    private var currentThresholdB = 50
    private var isNudgeEnabled = true
    private var hasSnoozedToday = false
    private var nudgedAToday = false
    private var nudgedBToday = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "✅ Accessibility service connected")

        serviceScope.launch { appSettings.thresholdA.collect { currentThresholdA = it } }
        serviceScope.launch { appSettings.thresholdB.collect { currentThresholdB = it } }
        serviceScope.launch { appSettings.nudgeEnabled.collect { isNudgeEnabled = it } }
        serviceScope.launch { appSettings.snoozedToday.collect { hasSnoozedToday = it } }
        serviceScope.launch { appSettings.nudgedAToday.collect { nudgedAToday = it } }
        serviceScope.launch { appSettings.nudgedBToday.collect { nudgedBToday = it } }
        
        serviceScope.launch { 
            appSettings.trackedApps.collect { apps ->
                trackedPackages = apps
                updateServiceInfoPackages()
                Log.i(TAG, "Monitoring packages: $trackedPackages")
            }
        }
    }

    private fun updateServiceInfoPackages() {
        val info = serviceInfo ?: AccessibilityServiceInfo()
        if (trackedPackages.isNotEmpty()) {
            info.packageNames = trackedPackages.toTypedArray()
        } else {
            // If none tracked, track a dummy package so we don't intercept everything
            info.packageNames = arrayOf("com.overscroll.dummy")
        }
        
        // Ensure other flags are kept
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_SCROLLED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        info.notificationTimeout = 100
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        // Use rootInActiveWindow to get the TRUE foreground app, ignoring background events
        val activeWindowPackage = rootInActiveWindow?.packageName?.toString()
        if (activeWindowPackage != null && !activeWindowPackage.startsWith("com.android.systemui") && !activeWindowPackage.contains("inputmethod")) {
            repository.setAppActive(activeWindowPackage)
        }

        val eventPackageName = event.packageName?.toString() ?: ""
        if (!trackedPackages.contains(eventPackageName)) return
        val trackedApp = AppTrackerConfig.getAppByPackage(eventPackageName) ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(event, trackedApp)
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                handleViewScrolled(event, trackedApp)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleContentChanged(event, trackedApp)
            }
        }
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent, trackedApp: TrackedApp) {
        val className = event.className?.toString() ?: return
        val wasInFeed = isInFeedViewer[trackedApp.packageName] ?: false

        val nowInFeed = if (trackedApp.classNames.isEmpty()) {
            true // If no classNames known, assume we might be in feed
        } else {
            trackedApp.classNames.any { knownName -> className.contains(knownName, ignoreCase = true) }
        }

        isInFeedViewer[trackedApp.packageName] = nowInFeed

        if (nowInFeed != wasInFeed) {
            if (nowInFeed) {
                windowEnterTimestamps[trackedApp.packageName] = System.currentTimeMillis()
                Log.i(TAG, "📱 Entered ${trackedApp.displayName} viewer (class: $className)")
            } else {
                Log.i(TAG, "📱 Left ${trackedApp.displayName} viewer (class: $className)")
            }
        }
    }

    private fun handleViewScrolled(event: AccessibilityEvent, trackedApp: TrackedApp) {
        var sourceNode: AccessibilityNodeInfo? = null
        try {
            sourceNode = event.source
            val resourceId = sourceNode?.viewIdResourceName

            if (resourceId == null) {
                return
            }

            // Check if this scroll is from the known feed container
            val isFeedScroll = if (trackedApp.feedResourceIds.isEmpty()) {
                // Since the user wants to track both reels AND normal feed scrolling,
                // and modern apps use various unknown classes (Compose, custom views),
                // we accept any valid scroll event here if we don't have hardcoded IDs.
                true
            } else {
                trackedApp.feedResourceIds.contains(resourceId)
            }

            if (!isFeedScroll) {
                val unknownSet = unknownScrollSources.getOrPut(trackedApp.packageName) { mutableSetOf() }
                if (unknownSet.add(resourceId)) {
                    Log.d(TAG, "🔍 ${trackedApp.displayName}: Scroll from unrecognized view: $resourceId (class: ${event.className})")
                }
                return
            }

            // ── Scroll detected — apply debounce ──
            val now = System.currentTimeMillis()
            val lastTimestamp = lastScrollTimestamps[trackedApp.packageName] ?: 0L
            val elapsed = now - lastTimestamp

            if (elapsed < AppTrackerConfig.SCROLL_DEBOUNCE_MS) {
                Log.v(TAG, "${trackedApp.displayName}: Scroll debounced (${elapsed}ms < ${AppTrackerConfig.SCROLL_DEBOUNCE_MS}ms)")
                return
            }

            // Ignore programmatic scrolls that often happen right when the app/window opens
            val enterTime = windowEnterTimestamps[trackedApp.packageName] ?: 0L
            if (now - enterTime < 1000L) {
                Log.v(TAG, "${trackedApp.displayName}: Ignoring scroll immediately after window enter (programmatic)")
                return
            }

            lastScrollTimestamps[trackedApp.packageName] = now

            // ── Count it! ──
            repository.increment(trackedApp.packageName)
            // Wait for flow to emit, but we can optimistically sum the state flow
            val combinedCount = repository.todayCounts.value.values.sum() + 1 // +1 for the increment just dispatched
            Log.i(TAG, "🎬 ${trackedApp.displayName} counted! (from: $resourceId, elapsed: ${elapsed}ms)")

            // ── Check Nudges (apply to combined count) ──
            if (isNudgeEnabled && !hasSnoozedToday) {
                serviceScope.launch {
                    if (combinedCount >= currentThresholdA && combinedCount < currentThresholdB && !nudgedAToday) {
                        appSettings.setNudgedAToday(true)
                        NotificationHelper.sendNudgeNotification(this@ReelsAccessibilityService, "You've scrolled $combinedCount videos today. Maybe take a quick breather?")
                    } else if (combinedCount >= currentThresholdB && !nudgedBToday) {
                        appSettings.setNudgedBToday(true)
                        NotificationHelper.sendNudgeNotification(this@ReelsAccessibilityService, "You've hit $combinedCount videos today. Consider closing the app.")
                    }
                }
            }

        } finally {
            // Recycling is automatic on modern Android.
        }
    }

    private fun handleContentChanged(event: AccessibilityEvent, trackedApp: TrackedApp) {
        val inFeed = isInFeedViewer[trackedApp.packageName] ?: false
        if (!inFeed) return

        val source = event.source
        val resourceId = source?.viewIdResourceName

        if (resourceId != null) {
            Log.v(TAG, "Content changed in ${trackedApp.displayName}: $resourceId")
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "⚠️ Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "❌ Accessibility service destroyed")
        Log.i(TAG, "Final combined count for this session: ${repository.todayCounts.value.values.sum()}")

        unknownScrollSources.forEach { (pkg, ids) ->
            if (ids.isNotEmpty()) {
                Log.i(TAG, "📋 Unrecognized scroll sources for $pkg: $ids")
            }
        }
        serviceScope.cancel()
    }
}
