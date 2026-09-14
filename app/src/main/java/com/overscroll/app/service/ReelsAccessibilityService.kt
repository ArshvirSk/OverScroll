package com.overscroll.app.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.overscroll.app.config.InstagramConfig
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Accessibility service scoped to com.instagram.android for Reels scroll detection.
 * (PRD §5.2)
 *
 * Detection strategy:
 * 1. Listen for TYPE_VIEW_SCROLLED events from Instagram
 * 2. Check if the scroll source's resource-id matches the Reels ViewPager ID
 *    (from InstagramConfig — the single file to update when IG changes their UI)
 * 3. Apply debounce (400ms) to filter fast swipes / view recycler noise
 * 4. Increment the count via ScrollCountRepository
 *
 * All events are logged to Logcat under tag "ReelsA11yService" for debugging.
 * Use `adb logcat -s ReelsA11yService:V` to see all events during development.
 *
 * ⚠️ This service is system-managed — it CANNOT use Hilt @AndroidEntryPoint.
 * Instead, it accesses dependencies via ReelsAccessibilityEntryPoint.
 *
 * See: res/xml/accessibility_service_config.xml for the XML configuration.
 * See: config/InstagramConfig.kt for the resource IDs being matched.
 * See: skills/accessibility-service.md for the full skill reference.
 */
class ReelsAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ReelsA11yService"
    }

    /** Timestamp of the last counted scroll, for debouncing */
    private var lastScrollTimestamp = 0L

    /** Whether we believe the user is currently in the Reels viewer */
    private var isInReelsViewer = false

    /** Set of resource IDs we've seen but didn't match — logged for ID discovery */
    private val unknownScrollSources = mutableSetOf<String>()

    /** Hilt dependency — lazily initialized via EntryPoint */
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
        Log.i(TAG, "Monitoring: ${InstagramConfig.PACKAGE_NAME}")
        Log.i(TAG, "Primary Reels ID: ${InstagramConfig.REELS_VIEW_PAGER_ID}")
        Log.i(TAG, "Fallback IDs: ${InstagramConfig.REELS_FALLBACK_IDS}")
        Log.i(TAG, "Debounce: ${InstagramConfig.SCROLL_DEBOUNCE_MS}ms")

        serviceScope.launch { appSettings.thresholdA.collect { currentThresholdA = it } }
        serviceScope.launch { appSettings.thresholdB.collect { currentThresholdB = it } }
        serviceScope.launch { appSettings.nudgeEnabled.collect { isNudgeEnabled = it } }
        serviceScope.launch { appSettings.snoozedToday.collect { hasSnoozedToday = it } }
        serviceScope.launch { appSettings.nudgedAToday.collect { nudgedAToday = it } }
        serviceScope.launch { appSettings.nudgedBToday.collect { nudgedBToday = it } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        val packageName = event.packageName?.toString() ?: ""
        
        // Robustly track whether Instagram (or our app) is active
        if (packageName.isNotEmpty() && !packageName.startsWith("com.android.systemui") && !packageName.contains("inputmethod")) {
            val isIgActive = packageName == InstagramConfig.PACKAGE_NAME || packageName == "com.overscroll.app"
            repository.setInstagramActive(isIgActive)
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            handleWindowStateChanged(event)
        }

        // For all other event types, strictly filter to Instagram only
        if (packageName != InstagramConfig.PACKAGE_NAME) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                handleViewScrolled(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleContentChanged(event)
            }
        }
    }

    /**
     * Track whether the user is in the Reels viewer surface.
     * Uses class name matching against known Reels fragment/activity names.
     * This is a secondary signal — resource-id matching in handleViewScrolled
     * is the primary detection mechanism.
     */
    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val className = event.className?.toString() ?: return
        val wasInReels = isInReelsViewer

        isInReelsViewer = InstagramConfig.REELS_ACTIVITY_CLASS_NAMES.any { knownName ->
            className.contains(knownName, ignoreCase = true)
        }

        if (isInReelsViewer != wasInReels) {
            if (isInReelsViewer) {
                Log.i(TAG, "📱 Entered Reels viewer (class: $className)")
            } else {
                Log.i(TAG, "📱 Left Reels viewer (class: $className)")
            }
        }
    }

    /**
     * Primary detection: handle scroll events from the Reels ViewPager.
     *
     * Workflow:
     * 1. Get the source node's resource-id
     * 2. Match against InstagramConfig.REELS_VIEW_PAGER_ID (and fallbacks)
     * 3. Apply debounce window
     * 4. If matched + debounce passed → count it
     */
    private fun handleViewScrolled(event: AccessibilityEvent) {
        var sourceNode: AccessibilityNodeInfo? = null
        try {
            sourceNode = event.source
            val resourceId = sourceNode?.viewIdResourceName

            if (resourceId == null) {
                // Source node unavailable or has no resource ID — can't match
                Log.v(TAG, "Scroll event with no resource ID (class: ${event.className})")
                return
            }

            // Check if this scroll is from the Reels ViewPager
            val isReelsScroll = resourceId == InstagramConfig.REELS_VIEW_PAGER_ID ||
                resourceId in InstagramConfig.REELS_FALLBACK_IDS

            if (!isReelsScroll) {
                // Log unknown scroll sources for resource-id discovery.
                // This helps when Instagram updates change the ViewPager ID —
                // run the app, scroll through Reels, and check Logcat for these logs.
                if (unknownScrollSources.add(resourceId)) {
                    Log.d(
                        TAG,
                        "🔍 Scroll from unrecognized view: $resourceId " +
                            "(class: ${event.className}). " +
                            "If this is the Reels ViewPager, update InstagramConfig.kt"
                    )
                }
                return
            }

            // ── Reels scroll detected — apply debounce ──
            val now = System.currentTimeMillis()
            val elapsed = now - lastScrollTimestamp

            if (elapsed < InstagramConfig.SCROLL_DEBOUNCE_MS) {
                Log.v(TAG, "Scroll debounced (${elapsed}ms < ${InstagramConfig.SCROLL_DEBOUNCE_MS}ms)")
                return
            }

            lastScrollTimestamp = now

            // ── Count it! ──
            repository.increment()
            val count = repository.todayCount.value
            Log.i(TAG, "🎬 Reel #$count counted! (from: $resourceId, elapsed: ${elapsed}ms)")

            // ── Check Nudges ──
            if (isNudgeEnabled && !hasSnoozedToday) {
                serviceScope.launch {
                    if (count >= currentThresholdA && count < currentThresholdB && !nudgedAToday) {
                        appSettings.setNudgedAToday(true)
                        NotificationHelper.sendNudgeNotification(this@ReelsAccessibilityService, "You've scrolled $count Reels today. Maybe take a quick breather?")
                    } else if (count >= currentThresholdB && !nudgedBToday) {
                        appSettings.setNudgedBToday(true)
                        NotificationHelper.sendNudgeNotification(this@ReelsAccessibilityService, "You've hit $count Reels today. Consider closing the app.")
                    }
                }
            }

        } finally {
            // On API 26+ (our minSdk), AccessibilityNodeInfo recycling is automatic.
            // No manual recycle() needed.
        }
    }

    /**
     * Secondary signal: content changes in the Reels viewer.
     * Currently used only for debug logging. Could be used in the future
     * to improve detection accuracy (e.g., confirming a new Reel loaded).
     */
    private fun handleContentChanged(event: AccessibilityEvent) {
        if (!isInReelsViewer) return

        // Only log at VERBOSE level to avoid spam
        val source = event.source
        val resourceId = source?.viewIdResourceName
        // On API 26+, recycling is automatic — no manual recycle() needed.

        if (resourceId != null) {
            Log.v(TAG, "Content changed in Reels: $resourceId")
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "⚠️ Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "❌ Accessibility service destroyed")
        Log.i(TAG, "Final count for this session: ${repository.todayCount.value}")

        // Log all unknown scroll sources seen during this session
        if (unknownScrollSources.isNotEmpty()) {
            Log.i(
                TAG,
                "📋 Unrecognized scroll sources seen this session " +
                    "(check if any are the Reels ViewPager): $unknownScrollSources"
            )
        }
        serviceScope.cancel()
    }
}
