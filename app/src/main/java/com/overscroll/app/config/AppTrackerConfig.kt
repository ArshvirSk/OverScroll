package com.overscroll.app.config

import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for tracked apps and their view identifiers.
 *
 * Resource IDs for apps (other than Instagram) are currently placeholders
 * pending uiautomator dump discovery on a physical device.
 */
object AppTrackerConfig {

    val SUPPORTED_APPS = listOf(
        TrackedApp(
            packageName = "com.instagram.android",
            displayName = "Instagram",
            accentColor = Color(0xFFC13584), // IG Pink/Purple
            feedResourceIds = listOf(), // Uses fallback
            classNames = listOf() // Uses fallback
        ),
        TrackedApp(
            packageName = "com.google.android.youtube",
            displayName = "YouTube",
            accentColor = Color(0xFFFF0000), // YouTube Red
            feedResourceIds = listOf(), // Uses fallback
            classNames = listOf() // Uses fallback
        ),

        TrackedApp(
            packageName = "com.snapchat.android",
            displayName = "Snapchat Spotlight",
            accentColor = Color(0xFFFFFC00), // Snapchat Yellow
            feedResourceIds = listOf(), // TODO: Verify via uiautomator
            classNames = listOf() // TODO: Verify via uiautomator
        ),
        TrackedApp(
            packageName = "com.facebook.katana",
            displayName = "Facebook Reels",
            accentColor = Color(0xFF1877F2), // FB Blue
            feedResourceIds = listOf(), // Uses fallback
            classNames = listOf()
        ),
        TrackedApp(
            packageName = "com.twitter.android",
            displayName = "X (Twitter)",
            accentColor = Color(0xFF000000), // X Black
            feedResourceIds = listOf(), // Uses fallback
            classNames = listOf()
        )
    )

    fun getAppByPackage(packageName: String): TrackedApp? {
        return SUPPORTED_APPS.find { it.packageName == packageName }
    }

    /**
     * Minimum interval (ms) between counted scroll events.
     * Prevents double-counting from fast swipes or view recycler noise.
     */
    const val SCROLL_DEBOUNCE_MS = 400L
}
