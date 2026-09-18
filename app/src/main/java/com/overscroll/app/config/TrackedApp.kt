package com.overscroll.app.config

import androidx.compose.ui.graphics.Color

/**
 * Represents an app that Overscroll tracks.
 *
 * @param packageName The application package name (e.g. "com.instagram.android")
 * @param displayName The human-readable name of the app (e.g. "Instagram Reels")
 * @param accentColor The brand color for UI elements
 * @param feedResourceIds A list of potential resource-ids for the scrollable container.
 *                        Multiple IDs are supported in case of A/B testing or app updates.
 * @param classNames A list of known Activity/Fragment class names to confirm the user is in the feed.
 */
data class TrackedApp(
    val packageName: String,
    val displayName: String,
    val accentColor: Color,
    val feedResourceIds: List<String>,
    val classNames: List<String>
)
