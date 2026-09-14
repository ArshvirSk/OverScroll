# Overscroll — Product Requirements Document

**Name:** Overscroll
**Tagline:** See how many Reels you actually scrolled through today.
**Platform:** Android only (v1)
**Status:** Draft v1

---

## 1. Problem

People have no idea how much of their Instagram time goes specifically into Reels scrolling — screen time trackers show app-level totals, not per-feature usage. There's no lightweight, always-visible way to see a live count of Reels watched, in the moment, without opening a separate stats app.

## 2. Goal

Give the user a real-time, ambient counter of Reels scrolled — visible as a floating bubble and/or a status-bar notification — plus a simple history view, entirely offline and local to the device.

## 3. Target user

Personal use / sideload, single user, Android device, comfortable granting Accessibility and overlay permissions. Not a Play Store consumer product in v1 (see Risks).

## 4. Success criteria (v1)

- Count tracks real Reels scrolls with reasonable accuracy (some drift from replays/fast-swipes is acceptable and documented, not silently hidden)
- Live counter visible without opening the app (notification and/or overlay)
- Count and history persist across app restarts and device reboots
- Zero network permissions, zero data leaves the device

## 5. Features

### 5.1 Onboarding
- Explains why Accessibility Service, "Display over other apps," and notification permissions are needed
- Deep-links to each system settings screen
- Shows live permission status (granted/not granted) for each

### 5.2 Reels detection
- `AccessibilityService` scoped to `com.instagram.android`
- Detects scroll/state-change events inside the Reels viewer, increments count
- Config file isolating Instagram's Reels ViewPager resource-id, since it can change across IG app updates
- Known limitations documented in-app (replays, pre-loads, fast swipes may cause under/over-count)

### 5.3 Live counter — overlay bubble
- Small floating, draggable bubble showing current count
- Toggle on/off from app settings
- Tap to expand into a mini today/this-week summary

### 5.4 Live counter — notification
- Foreground service, ongoing notification, live count in the title/text
- Toggle on/off from app settings
- Tapping notification opens the app

### 5.5 History
- Room-backed daily/weekly totals
- Simple Compose bar chart: reels per day, last 7/30 days
- Session log (optional v1.1): start/end time of each scrolling session and count within it

### 5.6 Settings
- Reset today's count / reset all history
- Toggle overlay, toggle notification independently
- Daily auto-reset time (default midnight)

## 6. Non-functional requirements

- Fully offline, no network permissions, no analytics/telemetry SDKs
- Min SDK 26+
- Battery-conscious: AccessibilityService and foreground service should be lightweight, no polling loops
- Survives device reboot (service restarts, count is not lost)

## 7. Tech stack

- Kotlin, single-module Android project, Jetpack Compose UI
- `AccessibilityService` for detection
- `WindowManager` + `TYPE_APPLICATION_OVERLAY` for the bubble
- Foreground `Service` + `NotificationManager` for the status-bar counter
- Room (SQLite) for history persistence
- Jetpack DataStore for settings/running totals
- Kotlin Coroutines + Flow for real-time updates from service to UI
- Hilt for DI
- WorkManager for daily reset + service health check

## 8. Out of scope (v1)

- iOS (not technically possible — no equivalent accessibility/overlay APIs)
- Cloud sync / multi-device
- Any backend, account system, or analytics
- Google Play distribution (see Risks)
- Tracking any app other than Instagram

## 9. Risks

- **Google Play policy risk:** using AccessibilityService for non-accessibility purposes is against Play's stated policy for that permission and this app would likely be rejected or pulled if submitted as-is. v1 targets sideload/personal use only.
- **Fragility:** Instagram's internal view IDs are obfuscated and change across app updates, requiring periodic re-verification via uiautomator/Accessibility Scanner.
- **Accuracy:** scroll-event-based counting is a heuristic, not an exact count guaranteed to match "reels actually watched."

## 10. Roadmap

- **v1:** onboarding, detection, overlay bubble, notification, history, settings — all as scoped above
- **v1.1 (maybe):** per-session log, weekly digest notification
- **v2 (maybe):** track additional short-form surfaces (YouTube Shorts) behind a per-app toggle, if the accessibility approach generalizes cleanly