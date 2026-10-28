## Why

The stock Google TV home screen (`com.google.android.apps.tv.launcherx`) is full of ads and recommendations, and Google TV has no setting for picking a different launcher. The kitchen Chromecast HD (boreal, Android 14 / SDK 34) is the low-risk device to prove this out on, because a factory reset is an acceptable fallback there.

## What Changes

- New Android app: a minimal, ad-free TV launcher (Kotlin + Compose for TV). It registers as a HOME / LEANBACK_LAUNCHER activity and shows favorite rows on top, then a grid of all launchable TV apps with their banners, plus a clock. The background is a simple gradient.
- A HOME-takeover spike on the kitchen device that tries three approaches in order and stops at the first that works reliably:
  - **E**: assign the home role with `cmd role add-role-holder android.app.role.HOME`.
  - **B**: disable the stock launcher, plus the setup-wizard `RecoveryActivity` fallback, with `pm disable-user`.
  - **C**: an accessibility service that catches HOME and opens our launcher.
- Each takeover step comes with a documented, tested undo command.
- Uninstall Twilight (`com.urbandroid.lux`) from the kitchen device, which removes its accessibility service.
- A dev loop: a Google TV (android-34, arm64) emulator on the Mac for UI work, and wireless adb with mDNS port discovery for installing to the kitchen device.

## Capabilities

### New Capabilities
- `launcher-home-screen`: The launcher UI. Favorites rows, the app grid, clock, gradient background, D-pad focus, and launching apps.
- `home-takeover`: Making the launcher what the HOME button and boot land on, with the E → B → C approaches, undo paths, and recovery.
- `dev-loop`: Building, previewing in the Google TV emulator, and deploying to the kitchen device over wireless adb.

### Modified Capabilities
<!-- none: no existing specs -->

## Impact

- New Gradle Android project in this repo (minSdk/targetSdk 34, Compose for TV dependencies).
- Device-side changes to the kitchen Chromecast only: the home role, disabled system packages, an enabled accessibility service, and Twilight uninstalled. All of these can be undone, and a factory reset is the last resort.
- Laptop: download of the `system-images;android-34;google-tv;arm64-v8a` image and a new AVD.
- Out of scope: weather, family photos (the Google Photos API restrictions since March 2025 make this a separate decision), and the living-room Google TV Streamer.
