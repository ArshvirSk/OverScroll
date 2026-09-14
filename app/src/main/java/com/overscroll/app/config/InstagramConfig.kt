package com.overscroll.app.config

/**
 * Single source of truth for Instagram's Reels-related view identifiers.
 *
 * ⚠️ IMPORTANT: These IDs are derived from Instagram's internal view hierarchy
 * and WILL change across Instagram app updates. Before relying on detection:
 *
 * 1. Connect device via ADB
 * 2. Open Instagram → navigate to the Reels tab
 * 3. Run:  adb shell uiautomator dump /sdcard/ui_dump.xml
 * 4. Pull: adb pull /sdcard/ui_dump.xml
 * 5. Search for ViewPager/RecyclerView containers in the dump
 * 6. Update the constants below with the actual resource IDs found
 *
 * Last verified: NOT YET VERIFIED — update after device verification
 * Instagram version verified against: N/A
 */
object InstagramConfig {

    /** Package name of Instagram — stable across updates */
    const val PACKAGE_NAME = "com.instagram.android"

    /**
     * Resource ID of the Reels ViewPager container.
     * This is the primary element we monitor for scroll events.
     *
     * ⚠️ PLACEHOLDER — must be verified via uiautomator dump on a real device.
     * If the actual ID differs, update this constant and re-deploy.
     *
     * Common historical IDs:
     * - "com.instagram.android:id/clips_viewer_view_pager"
     * - "com.instagram.android:id/clips_tab_view_pager"
     */
    const val REELS_VIEW_PAGER_ID = "com.instagram.android:id/clips_viewer_view_pager"

    /**
     * Fallback resource IDs to check when the primary ID isn't found.
     * Instagram A/B tests different layouts, so multiple IDs may be valid.
     */
    val REELS_FALLBACK_IDS = listOf(
        "com.instagram.android:id/clips_viewer_view_pager",
        "com.instagram.android:id/clips_tab_view_pager",
    )

    /**
     * Minimum interval (ms) between counted scroll events.
     * Prevents double-counting from:
     * - Fast swipes that fire multiple TYPE_VIEW_SCROLLED events
     * - View recycler rebinds that trigger TYPE_WINDOW_CONTENT_CHANGED
     *
     * Tuned to ~400ms: fast enough to catch real swipes, slow enough to debounce noise.
     */
    const val SCROLL_DEBOUNCE_MS = 400L

    /**
     * Class names associated with the Reels viewer.
     * Used as a secondary signal to confirm we're inside the Reels surface
     * (not the main feed, stories, or explore grid).
     */
    val REELS_ACTIVITY_CLASS_NAMES = listOf(
        "com.instagram.reels.fragment.ReelsViewerFragment",
        "com.instagram.clips.fragment.ClipsViewerFragment",
    )
}
