# Overscroll

**See how many Reels you actually scrolled through today.**

Overscroll is an Android app that gives you a real-time, ambient counter of the Instagram Reels you've watched. Instead of checking screen time later, you get an expressive floating mascot that reacts live as you scroll. 

The app works entirely offline—zero data ever leaves your device.

---

## Features

- **Floating Mascot Overlay:** A friendly blob that floats over Instagram. It bounces when you scroll, blinks to feel alive, and changes expressions (Content → Tired → Worn Out) as your count goes up.
- **Real-time Tracking:** Accurate, live counting of Reels scrolls using Android's Accessibility Services.
- **Nudge Notifications:** Get gentle, snooze-able nudge notifications when you cross your custom scroll thresholds.
- **Detailed History:** See your scroll history segmented by Today, Week, Month, and Year in an elegant card-based UI.
- **Fully Local & Private:** No internet permission required. All data is stored locally on your device.

---

## Prerequisites

- An Android device (v1 is Android-only).
- Developer Options & USB Debugging enabled on your device (if building and installing via Android Studio).

---

## Installation Guide

Since Overscroll relies on system-level Accessibility and Overlay permissions, it's designed to be sideloaded.

### 1. Download and Install the APK
1. Go to the [Releases](../../releases) page of this repository.
2. Download the latest `app-release.apk` directly to your Android device.
3. Tap on the downloaded file to install it. (You may need to allow your browser or file manager to "Install unknown apps").
### 2. Grant Required Permissions

Once installed, Overscroll requires two critical permissions to function. The app will prompt you for these on the home screen.

#### Permission 1: Display Over Other Apps
This allows the scroll mascot to float over Instagram.
1. Open Overscroll and tap the **"Display over other apps"** prompt.
2. Find **Overscroll** in the list.
3. Toggle **Allow display over other apps** to ON.

#### Permission 2: Accessibility Service
This allows Overscroll to detect when you swipe to a new Reel.
1. Open Overscroll and tap the **"Enable Accessibility Service"** prompt.
2. You will be taken to your device's Accessibility Settings.
3. Look for **Installed apps** or **Downloaded apps**.
4. Tap **Overscroll** and toggle it ON.

> **⚠️ IMPORTANT FOR ANDROID 13+ USERS (Restricted Settings)**
> Android 13 and newer heavily restricts Accessibility services for sideloaded apps. If the toggle is grayed out and says "Restricted Setting":
> 1. Go to your phone's main **Settings** app.
> 2. Go to **Apps** > **Overscroll**.
> 3. Tap the **three-dot menu** in the top right corner.
> 4. Select **Allow restricted settings**.
> 5. You will now be able to enable the Accessibility Service.

### 3. Battery Optimization (Samsung / Xiaomi Users)
Some manufacturers aggressively kill background apps. If the mascot disappears or stops counting:
1. Go to **Settings > Apps > Overscroll > Battery**.
2. Set it to **Unrestricted**.

---

## Usage & Settings

- **Start Scrolling:** Once permissions are granted, simply open Instagram and go to the Reels tab. The Overscroll mascot will appear and track your scrolls.
- **Drag & Snap:** You can drag the mascot anywhere on the left or right edges of your screen.
- **Custom Nudges:** Open the Overscroll app and go to the **Settings** tab. Here you can configure your "Tired" (Threshold A) and "Worn Out" (Threshold B) limits, and toggle Nudge notifications on or off.

---

## Privacy Note
Overscroll does not request the `INTERNET` permission in its AndroidManifest. It is physically impossible for the app to send your data anywhere. Your scroll history is yours alone.



## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
