# Accessibility Service — Skill Reference

## Overview
`ReelsAccessibilityService` extends `AccessibilityService`, scoped to `com.instagram.android` only.
Its job: detect Reels scroll events, debounce them, and increment the count via `ScrollCountRepository`.

## XML Config (`res/xml/accessibility_service_config.xml`)
- `packageNames="com.instagram.android"` — restricts event delivery to Instagram only
- `accessibilityEventTypes`: `typeViewScrolled | typeWindowContentChanged | typeWindowStateChanged`
- `flagReportViewIds` — essential for reading `viewIdResourceName` from `AccessibilityNodeInfo`
- `canRetrieveWindowContent="true"` — needed to traverse the view tree
- `notificationTimeout="100"` — 100ms minimum between event deliveries

## Resource-ID Verification (CRITICAL)
Instagram obfuscates and changes view IDs across updates. **Never guess.**

### Verification steps:
```bash
adb shell uiautomator dump /sdcard/ui_dump.xml
adb pull /sdcard/ui_dump.xml
# Open ui_dump.xml, search for ViewPager/RecyclerView near Reels content
# Update InstagramConfig.REELS_VIEW_PAGER_ID with the actual ID found
```

### Current placeholder:
`com.instagram.android:id/clips_viewer_view_pager` — **UNVERIFIED**

### Alternative discovery method:
1. Install the app with the current placeholder IDs
2. Enable the accessibility service
3. Open Instagram and scroll through Reels
4. Run: `adb logcat -s ReelsA11yService:D`
5. Look for "Scroll from unrecognized view" log entries — these show the actual resource IDs
6. Update `InstagramConfig.REELS_VIEW_PAGER_ID` with the correct ID

## Debouncing Strategy
- Track `lastScrollTimestamp` (System.currentTimeMillis)
- Ignore events within `InstagramConfig.SCROLL_DEBOUNCE_MS` (400ms) of the last counted scroll
- This handles: fast double-swipes, ViewPager over-scroll, content preloading events

## Hilt Integration
AccessibilityService is system-managed, so we use `EntryPointAccessors.fromApplication()`:

```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReelsAccessibilityEntryPoint {
    fun scrollCountRepository(): ScrollCountRepository
}

// In the service:
private val repository by lazy {
    EntryPointAccessors.fromApplication(
        applicationContext,
        ReelsAccessibilityEntryPoint::class.java,
    ).scrollCountRepository()
}
```

## Data Flow
```
Instagram scroll
    → Android Accessibility Framework
        → onAccessibilityEvent(event)
            → TYPE_VIEW_SCROLLED?
                → event.source.viewIdResourceName matches InstagramConfig?
                    → Debounce window passed?
                        → ScrollCountRepository.increment()
                            → StateFlow update (optimistic, instant)
                            → DataStore persist (background)
                            → Overlay observes → updates bubble
                            → Notification observes → updates notification
                            → HomeScreen observes → updates UI
```

## API Gotchas
1. **System-managed lifecycle**: Can't bind/start — user toggles in Accessibility Settings
2. **No Hilt @AndroidEntryPoint**: Use `EntryPointAccessors` (see above)
3. **Main thread**: `onAccessibilityEvent` runs on main thread — keep processing fast
4. **event.source can be null**: IPC call, may fail. Always null-check
5. **Recycle source nodes**: Call `sourceNode.recycle()` in a finally block
6. **Survives app process death**: Runs in app process but started by system
7. **Testing**: `adb logcat -s ReelsA11yService:V` for all events
8. **Programmatic enable**: `adb shell settings put secure enabled_accessibility_services com.overscroll.app/.service.ReelsAccessibilityService`

## Logcat Tags
- `ReelsA11yService` — all service logs
  - `I`: Service lifecycle, counted Reels, entering/leaving Reels viewer
  - `D`: Unrecognized scroll sources (for ID discovery)
  - `V`: Debounced events, content changes (verbose, high-volume)
