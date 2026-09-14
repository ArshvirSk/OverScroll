# Overscroll — Project Structure

## Architecture Overview
Single-module Android app, all Compose UI, dark theme, fully offline.

## Package Layout
```
com.overscroll.app/
├── OverscrollApp.kt              @HiltAndroidApp, custom WorkManager config
├── MainActivity.kt               @AndroidEntryPoint, edge-to-edge, hosts NavHost
├── config/
│   └── InstagramConfig.kt        ⚠️ Reels ViewPager resource-ID (MUST verify on device)
├── data/
│   ├── db/                        Room database (Phase 5)
│   └── settings/                  DataStore preferences (Phase 6)
├── di/
│   └── AppModule.kt              Hilt module (grows with each phase)
├── overlay/                       Floating bubble service (Phase 3)
├── receiver/
│   └── BootReceiver.kt           Restarts services after reboot
├── service/
│   ├── ReelsAccessibilityService  Scroll detection (Phase 2)
│   ├── CounterForegroundService   Notification counter (Phase 4)
│   └── ScrollCountRepository      Central count state (StateFlow<Int>)
├── ui/
│   ├── home/HomeScreen.kt        Dashboard with big counter
│   ├── history/HistoryScreen.kt  Bar chart (Phase 5)
│   ├── navigation/AppNavigation  NavHost with conditional start
│   ├── onboarding/               Permission flow (Phase 1)
│   ├── settings/SettingsScreen   Toggles and resets (Phase 6)
│   └── theme/                    Material3 dark theme
└── worker/                        WorkManager jobs (Phase 7)
```

## Key Design Decisions
- **Always-dark theme**: No light mode toggle. Brand is purple/cyan/pink on deep dark.
- **In-memory → DataStore → Room**: Count flows from AccessibilityService → StateFlow → DataStore (current total) → Room (daily history).
- **Separate services for overlay and notification**: Independent toggle per PRD §5.6.
- **No internet permission**: Enforced in manifest, checked in PRD §6 compliance.
- **KSP not KAPT**: All annotation processing (Room, Hilt) uses KSP for faster builds.

## Gotchas
- `enableEdgeToEdge()` requires manual `statusBarsPadding()` / `navigationBarsPadding()` in Compose.
- The Gradle wrapper JAR must be generated (`gradle wrapper`) before `./gradlew` works — Android Studio handles this automatically on project import.
- Hilt + WorkManager requires custom `Configuration.Provider` in the Application class and disabling the default `WorkManagerInitializer` in the manifest.
