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

**Scope note (as of v3):** the product has grown past pure counting into an emotionally-expressive mascot, nudge notifications, and a local reel-save/organize tool. None of that is required to satisfy §1's original problem statement on its own — it's addressed via the `counterOnly` / `full` build-flavor split in §5.1, which keeps a pure "just the counter" APK available so the original goal stays intact for anyone who wants it, without that variant even containing the extra code.

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
- **(v3) Build variant (not a runtime toggle):** Overscroll ships as two Android product flavors from one codebase — **`counterOnly`** and **`full`** — rather than a Settings toggle
  - **`counterOnly`:** compiles in detection, plain bubble/notification, and history only. The mascot emotion/animation code, nudge notification logic, and reel-capture/Saved-collection code are entirely absent from this build — not hidden, not present in the APK at all
  - **`full`:** compiles in everything — emotion states and animations (§5.3), nudge notifications (§5.5), reel capture/save/organize (§5.8, §5.9)
  - Both flavors share the same `applicationId` and are signed with the same key, so installing one over the other is treated as a normal app update rather than a fresh install — Room/DataStore data (history, saved reels, settings) survives a switch between flavors
  - Which apps are tracked (§5.2) is a runtime setting in both flavors, independent of which flavor is installed
  - This is the direct fix for scope-creep concerns: someone who only wants the counter installs `counterOnly` and the extra code genuinely isn't there, while `full` keeps everything built without needing a hidden-behind-a-flag compromise

### 5.2 Multi-app scroll detection (v2)
- Generalized from a single hardcoded Instagram target to a `TrackedApp` config model: `packageName`, `displayName`, `accentColor`, `feedResourceId` (the reel/short container view-id for that app)
- v2 tracked apps: Instagram Reels, YouTube Shorts, TikTok, Snapchat Spotlight, Facebook Reels — each with its own verified resource-id, found via uiautomator/Accessibility Scanner the same way Instagram's was
- Single `AccessibilityService` scoped to all enabled tracked packages (`packageNames` filter list, not a single string), routes each scroll event to the matching `TrackedApp` config
- Each app individually toggleable on/off in Settings — a disabled app is fully excluded from the accessibility event filter, not just hidden in the UI
- Per-app resource-ids live in one config file/table so future apps or IG-style ID changes only require updating that one place
- Known limitations documented per-app (some apps' feeds may not isolate as cleanly as Instagram's ViewPager — flag any app where scroll-event detection turns out unreliable rather than shipping a silently inaccurate count for it)

### 5.3 Live counter — overlay bubble
- Small floating, draggable bubble showing current count, with a simple two-eyes-and-mouth mascot face (current v1 shape: teal circle, dot eyes, flat mouth)
- Toggle on/off from app settings
- Tap to expand into a mini today/this-week summary
- **Emotional states (v1.2):** the mascot's face changes expression based on configurable count thresholds against the day's total — e.g. neutral/content early on, a slightly more tired or wide-eyed expression past a mid threshold, a visibly worn-out expression past a high threshold. Thresholds are editable in Settings, with sensible defaults (e.g. 0–20 content, 20–50 tired, 50+ worn out).
- **Micro-animations (v1.2):** a small bounce/squash on each count increment; an idle animation (slow blink every few seconds) so the bubble reads as alive rather than static; a brief "shake" or attention animation when a threshold is first crossed.
- **Multi-app aggregation (v2):** the bubble's main number is the combined count across all enabled tracked apps; tapping the expanded summary shows a per-app breakdown (Instagram / YouTube Shorts / TikTok / Snapchat Spotlight / FB Reels), each with its own small accent-colored count. Emotion thresholds apply to the combined total, not per-app.
- **Interaction change (v3):** tap continues to open the expanded mini-summary, which now also shows a "Current reel" card (see §5.8) when the foreground app is a tracked app and a reel is actively on screen. Long-press the bubble to instantly capture-and-save the current reel without opening the expanded view (quick-save shortcut for power use).

### 5.8 Reel info capture & save (v3)
- When a tracked app is in the foreground and a reel/short is on screen, read the visible metadata via the AccessibilityService's node tree: creator handle, caption/description text, and audio/sound label, wherever each app exposes them as inspectable text nodes
- Each field's resource-id is looked up per app (same uiautomator process as feed detection) and isolated in the per-app config — some apps may not expose all three fields cleanly; capture whatever is reliably available per app and leave the rest blank rather than guessing
- Bubble's expanded view shows this info as a "Current reel" card with a **Save** button
- Save writes a local record only — nothing is sent to Instagram or any other app, and no native "Save" action is triggered in the source app
- If a permalink/share-link is capturable as text on screen, store it too; if not capturable, don't fabricate one — leave it null
- No screenshot/thumbnail capture in v3 (noted as a possible future enhancement, not built now)

### 5.9 Saved collection (organize, v3)
- New "Saved" screen in the main app listing everything captured via §5.8
- Each saved item: creator handle, caption, audio label, source app, saved timestamp, permalink (if captured)
- User can add freeform tags to any saved item, and create named folders/collections to file items into (an item can have multiple tags but belongs to at most one folder)
- Search/filter the saved list by tag, folder, source app, or creator handle
- Delete individual saved items or entire folders
- This is entirely local organization within Overscroll — it does not read, modify, or sync with Instagram's (or any tracked app's) own native saved/collections feature

### 5.4 Live counter — notification
- Foreground service, ongoing notification, live count in the title/text
- Toggle on/off from app settings
- Tapping notification opens the app
- (v2) Notification shows the combined total; expanding it (expanded notification style) lists the per-app breakdown

### 5.5 Nudge notifications (v1.2)
- Separate, dismissible notification (distinct from the always-on live-count notification) fired once per threshold crossing per day — not repeating, not spammy
- Copy is gentle/observational, not shaming — e.g. noting the day's count has passed a threshold, framed as information rather than a scolding
- Each nudge has a "Snooze nudges today" action so the user can turn them off for the rest of the day without opening the app
- Fully toggleable off in Settings independent of the live overlay/notification

### 5.6 History
- Room-backed daily/weekly totals
- Simple Compose bar chart: reels per day, last 7/30 days
- Session log (optional v1.1): start/end time of each scrolling session and count within it
- (v2) Per-app filter/tab on the history screen (All apps combined, or one specific tracked app), each count row/session tagged with which app it came from in the Room schema

### 5.7 Settings
- Reset today's count / reset all history
- Toggle overlay, toggle notification independently
- Daily auto-reset time (default midnight)
- (v1.2) Edit mascot emotion thresholds; toggle nudge notifications independently of the live count notification
- (v2) Per-app on/off toggle for each tracked app (Instagram, YouTube Shorts, TikTok, Snapchat Spotlight, FB Reels); "Reset all history" clarified to reset all apps, with an option to reset a single app's history instead
- (v3) Toggle reel-info capture on/off independently per app (in case a field lookup proves unreliable for one app); manage folders (rename/delete) from Settings as well as from the Saved screen
- (v3) No in-app mode switch — moving between `counterOnly` and `full` means installing the other build variant (§5.1), which preserves local data since both share the same `applicationId`

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
- Gradle product flavors (`counterOnly`, `full`) sharing one `applicationId`, splitting mascot-emotion/nudge/reel-capture code into flavor-specific source sets rather than a runtime flag

## 8. Out of scope (v2)

- iOS (not technically possible — no equivalent accessibility/overlay APIs)
- Cloud sync / multi-device
- Any backend, account system, or analytics
- Google Play distribution (see Risks)
- Tracking any app beyond the v2 set (Instagram, YouTube Shorts, TikTok, Snapchat Spotlight, FB Reels) — Pinterest, X, and regional apps (Moj, Josh, etc.) considered but deferred, each needs its own resource-id research and feed-detection reliability check before being added
- (v3) Triggering any native "Save" action inside a tracked app, or reading/modifying that app's own saved/collections feature — Overscroll's save/organize system is entirely separate and local-only
- (v3) Screenshot or thumbnail capture of reels

## 9. Risks

- **Google Play policy risk:** using AccessibilityService for non-accessibility purposes is against Play's stated policy for that permission and this app would likely be rejected or pulled if submitted as-is. v1 targets sideload/personal use only.
- **Fragility:** each tracked app's internal view IDs are obfuscated and change across app updates, requiring periodic re-verification via uiautomator/Accessibility Scanner — this maintenance burden now multiplies by 5 tracked apps instead of 1.
- **Accuracy:** scroll-event-based counting is a heuristic, not an exact count guaranteed to match "reels actually watched," and some apps' feed structure may prove less reliable to detect than Instagram's.
- **Battery/perf:** a single AccessibilityService now filters events across 5 packages instead of 1 — verify this doesn't measurably increase battery draw versus v1.

## 10. Roadmap

- **v1:** onboarding, detection, overlay bubble, notification, history, settings — all as scoped above (shipped)
- **v1.1 (maybe):** per-session log, weekly digest notification
- **v1.2:** mascot emotional states + micro-animations on the overlay bubble, threshold-based nudge notifications (§5.3, §5.5) (shipped)
- **v2 (current):** multi-app tracking — Instagram, YouTube Shorts, TikTok, Snapchat Spotlight, Facebook Reels — with per-app toggles and history breakdown (§5.2–§5.7)
- **v3 (next):** interactive bubble — capture current reel's creator/caption/audio via the accessibility node tree, local-only save, and a Saved screen with tags/folders/search for organizing what's been saved (§5.8, §5.9)
- **v4 (maybe):** additional apps (Pinterest, regional short-video apps) if the per-app config model proves easy to extend