# Overlay Window — Skill Reference

## Overview
`OverlayBubbleService` renders a floating, draggable bubble showing the live Reels count.
Tap to expand into a mini summary card. Runs as a foreground service with TYPE_APPLICATION_OVERLAY.

## Key Implementation Details

### Window Setup
```kotlin
WindowManager.LayoutParams(
    WRAP_CONTENT, WRAP_CONTENT,
    TYPE_APPLICATION_OVERLAY,
    FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_NO_LIMITS,
    PixelFormat.TRANSLUCENT
)
```
- `TYPE_APPLICATION_OVERLAY`: Requires SYSTEM_ALERT_WINDOW permission (granted via Settings, not runtime)
- `FLAG_NOT_FOCUSABLE`: Overlay doesn't steal focus/keyboard from the underlying app
- `FLAG_LAYOUT_NO_LIMITS`: Allows the bubble to be positioned at screen edges

### Using Compose in WindowManager
When drawing floating overlays using `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`, standard Android `View`s work out of the box, but a Jetpack `ComposeView` requires a `LifecycleOwner`, `ViewModelStoreOwner`, and `SavedStateRegistryOwner` attached to the view tree.

Since services like `LifecycleService` do not provide a `ViewModelStoreOwner` or `SavedStateRegistryOwner`, you must create a custom class to provide them, and attach them to the `ComposeView` before adding it to the `WindowManager`.

```kotlin
// Create a ComposeLifecycleOwner:
class ComposeLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    // Implement using LifecycleRegistry, SavedStateRegistryController, and ViewModelStore
    // See com.overscroll.app.overlay.ComposeLifecycleOwner for full implementation
}

// Attach in your service:
val lifecycleOwner = ComposeLifecycleOwner()
lifecycleOwner.attachToLifecycle()

val composeView = ComposeView(context).apply {
    setViewTreeLifecycleOwner(this, lifecycleOwner)
    setViewTreeViewModelStoreOwner(this, lifecycleOwner)
    setViewTreeSavedStateRegistryOwner(this, lifecycleOwner)
    setContent { /* Compose UI */ }
}
```

If your Compose layout uses scaling animations (e.g., bouncing), the bounds of the `WindowManager` layout may clip the drawing. Wrap your content inside a Compose `Box` with padding to add transparent space to the window to prevent clipping.

### Drag + Tap Detection
Touch events on the overlay use a threshold-based approach:
- `ACTION_DOWN`: Record initial position
- `ACTION_MOVE`: If moved > 8dp → it's a drag, update window position
- `ACTION_UP`: If didn't move → it's a tap, toggle expand/collapse

### Edge Snapping
After drag ends, the bubble animates to the nearest screen edge.
Uses `ValueAnimator` with `OvershootInterpolator` for a satisfying snap feel.

### Foreground Service
Required on API 26+ for background execution. Uses:
- `ServiceCompat.startForeground()` for API compatibility
- `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` on API 34+
- `IMPORTANCE_MIN` notification channel (barely visible)

### Count Observation
Uses `LifecycleService` for lifecycle-aware `lifecycleScope.launch { flow.collect {} }`.

## API Gotchas
1. **SYSTEM_ALERT_WINDOW**: Can't be requested at runtime like normal permissions. Must deep-link to `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`. Check with `Settings.canDrawOverlays(context)`.
2. **WindowManager.addView() crashes if called twice** without removeView() first.
3. **Service must call startForeground() within 5 seconds** of `startForegroundService()` or the system throws an exception.
4. **On API 34+**: Must specify `foregroundServiceType` in both the manifest AND the `startForeground()` call.
5. **removeView() in onDestroy()**: Always try/catch — the view might already be removed.
6. **Display metrics for edge snapping**: Use `resources.displayMetrics.widthPixels` for screen width.

## Testing
```bash
# Start overlay via ADB (if app is in foreground)
adb shell am start-foreground-service -n com.overscroll.app/.overlay.OverlayBubbleService

# Stop overlay
adb shell am stopservice -n com.overscroll.app/.overlay.OverlayBubbleService

# Check if overlay permission is granted
adb shell appops get com.overscroll.app SYSTEM_ALERT_WINDOW
```
