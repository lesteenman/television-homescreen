## Context

The kitchen device is a Chromecast with Google TV HD (`boreal`) running Android 14 (SDK 34), build `UTTC.250917.004`, security patch 2025-10-01. It outputs 1920×1080 at density 320. Wireless debugging is paired; the connect port is advertised over mDNS (`_adb-tls-connect._tcp`) and changes after a reboot.

Current HOME resolution, read from the device:

| Priority | Activity | Notes |
|---|---|---|
| 2 | `com.google.android.apps.tv.launcherx/.home.HomeActivity` | Current home screen; an updated system app |
| 1 | `com.google.android.tungsten.setupwraith/.RecoveryActivity` | Takes over if launcherx is disabled |
| -1000 | `com.android.tv.settings/.system.FallbackHome` | Last resort |

Other relevant packages: `tvlauncher` (the legacy Android TV launcher; not a HOME handler today), `katniss` (voice/Assistant), `dreamx` + `backdrop` (screensaver), `tvrecommendations` (already disabled). Twilight (`com.urbandroid.lux`) currently holds the only enabled accessibility service and will be uninstalled.

A third-party app can't declare an intent-filter priority above 0, so our HOME activity can never outrank launcherx on priority alone.

## Goals / Non-Goals

**Goals:**
- Pressing HOME, and booting, lands on our launcher on the kitchen device, consistently.
- A calm, fast home screen: favorites rows, the full app grid, a clock, and a gradient background.
- Every device change can be undone with a documented adb command.
- A fast UI iteration loop on the Mac without touching the TV.

**Non-Goals:**
- Weather, photos, notifications, or a replacement for the quick-settings panel.
- Distribution (Play Store), signing for release, or multi-user support.
- The living-room Google TV Streamer.
- Live TV / HDMI inputs. A Chromecast has none.

## Decisions

### D1: HOME takeover order E → B → C, stopping at the first approach that sticks
- **E (home role):** `adb shell cmd role add-role-holder android.app.role.HOME me.steenman.tvlauncher` (application ID `me.steenman.tvlauncher`). Nothing is disabled, so this is the least invasive option, and RoleManager handles HOME resolution on Android 10+. Risk: Google TV may ignore the role, or launcherx may take it back. Undo: `cmd role remove-role-holder …` / `add-role-holder … com.google.android.apps.tv.launcherx`.
- **B (disable):** `pm disable-user --user 0 com.google.android.apps.tv.launcherx`, then the same for `com.google.android.tungsten.setupwraith` if RecoveryActivity wins. Undo: `pm enable` for each. Disabling setupwraith may affect OTA or setup flows, so check both after an OTA.
- **C (accessibility):** an `AccessibilityService` in the same APK. It watches for launcherx's `HomeActivity` coming to the foreground (`TYPE_WINDOW_STATE_CHANGED`) and starts our launcher. Key-event filtering for HOME is a secondary option, since many TV builds don't deliver KEYCODE_HOME to accessibility services. Enabling it: `settings put secure enabled_accessibility_services <pkg>/<svc>` plus `accessibility_enabled 1`.
- "Sticks" means it survives 10 HOME presses, a reboot, and 24 hours of normal use, including any app or launcherx updates in that window.
- Alternative considered: using only C from the start. Rejected because the stock launcher flashes before the redirect, and accessibility services can be killed.

### D2: Single APK, Kotlin + Compose for TV
- `androidx.tv:tv-material` for focus-aware cards and `LazyRow`/`LazyVerticalGrid` for the layout. minSdk = targetSdk = 34, since there is one target device.
- Alternatives: Leanback (legacy, fragment-heavy) and Flutter as used by FLauncher (an extra toolchain). Compose for TV is Google's current direction and keeps everything in Kotlin.
- The accessibility service (approach C) lives in the same APK. It is only enabled if approach C is needed.

### D3: App discovery via PackageManager
- Query `ACTION_MAIN` + `CATEGORY_LEANBACK_LAUNCHER`, declared in `<queries>` so `QUERY_ALL_PACKAGES` isn't needed. Use `getActivityBanner`, falling back to `getApplicationBanner`, falling back to the icon on a tinted card.
- Exclude our own launcher.
- Refresh on `ACTION_PACKAGE_ADDED/REMOVED/CHANGED` via a dynamically registered receiver while the launcher is in the foreground, and on `onResume`.

### D4: Favorites stored in DataStore
- An ordered list of component names in Preferences DataStore.
- Long-press (or the menu key) on a card opens a small action sheet: add to or remove from favorites, move left or right, open app info.
- The favorites row is hidden when empty. On first run it is pre-seeded with nothing; the user picks.

### D5: Visuals
- A full-screen vertical/diagonal gradient drawn with a Compose `Brush` (two to three dark tones). No images, so there's nothing to load.
- The clock sits top-right in the locale's 24-hour or 12-hour format, updated every minute (`ACTION_TIME_TICK`).
- Layout: clock header → favorites row → "All apps" grid of 16:9 banner cards. Overscan-safe margins of 48 dp horizontal and 27 dp vertical.

### D6: Back and HOME behaviour inside the launcher
- BACK on the home screen does nothing and stays put; it never exits to the stock launcher.
- HOME while already home scrolls to the top and focuses the first favorite.

### D7: Dev loop
- Emulator: `system-images;android-34;google-tv;arm64-v8a`, AVD "gtv-34" with a 1080p TV profile, used for UI work. Takeover experiments happen only on the real device, because the emulator's launcher and packages differ from production.
- Device: a `scripts/` helper resolves the current connect port from `adb mdns services`, then runs `adb connect` and `./gradlew installDebug`.
- The takeover helpers (`scripts/takeover-{e,b,c}.sh`, `scripts/restore-stock.sh`) are plain shell scripts, so they can be read and undone easily.

### Spike results

- **E (home role), 2026-10-28: does not work.** `cmd role add-role-holder` succeeds, and our activity becomes the preferred HOME activity (`dumpsys package` shows it), yet HOME opened launcherx 3 out of 3 times. When the top HOME candidate has a strictly higher intent-filter priority (launcherx: 2), Android picks it without looking at preferred activities. A third-party app can't declare more than 0, so the role can't win while launcherx is enabled. The same rule means setupwraith (priority 1) will outrank us under B unless it is disabled too.

- **B, launcherx only, 2026-10-28: does not work.** setupwraith's `RecoveryActivity` (priority 1) takes over HOME, and HOME pressed inside an app does nothing visible.
- **B + setupwraith disabled, 2026-10-28: works.** After a reboot the TV boots straight into our launcher. HOME landed on our launcher 10 out of 10 times, pressed from YouTube, Netflix and Settings. Our app is also the HOME role holder. Voice search (`KEYCODE_SEARCH` → katniss keyboard search) and the Assistant overlay (`KEYCODE_ASSIST`) still work, and so does Settings. The screensaver setting is unchanged (`dreamx/.service.Backdrop`), but it can't be started from the shell without root. Still to check: wait for the idle timeout.
- **Unexplained:** right after setupwraith was disabled, the TV dropped off Wi-Fi for several minutes. It did not reboot (uptime continued), and wireless debugging came back on a new port after being switched on again by hand. The logs had already rotated, so the cause is unknown. Watch for this happening again.

## Risks / Trade-offs

- [Google TV ignores or resets the HOME role (E)] → Fall through to B, and record what happened in this design.
- [An OTA or Play update re-enables launcherx (B)] → Pair B with C as a watchdog. Re-check after updates.
- [setupwraith disabled breaks OTA, factory-reset setup, or account flows] → Disable it only if RecoveryActivity is actually observed winning. Re-enable before any factory reset or OTA.
- [We lose the stock quick-settings side panel and the input switcher] → Accepted for the PoC. The Settings app is reachable from our launcher through a "Settings" tile (`Settings.ACTION_SETTINGS`).
- [Voice button or Assistant behaviour changes once launcherx is disabled] → Test it. Katniss is a separate package, so it may keep working.
- [The device is left with no working HOME handler] → Undo scripts are tested before each step. Wireless adb stays paired. Factory reset is the agreed last resort.
- [Wireless debugging is lost after a reboot, so we can't reach adb] → **Confirmed 2026-10-28:** after a reboot the TV advertises no adb service, and wireless debugging has to be switched on again by hand (Settings → System → Developer options → Wireless debugging). The pairing itself survives. So after any reboot, recovery depends on reaching Settings with the remote, not on adb. Before B or C, check that Settings can be reached from whatever HOME lands on (our launcher's Settings button, or the stock launcher).

## Migration Plan

1. Uninstall Twilight from the kitchen device: `pm uninstall --user 0 com.urbandroid.lux`.
2. Install the launcher APK. Verify it shows in the emulator and on the device when launched manually.
3. Try E. If it sticks, stop.
4. Otherwise run `restore-stock`, then try B. Disable setupwraith only if needed.
5. If B is unstable, add C as a watchdog.
6. Rollback at any point: `scripts/restore-stock.sh`, which re-enables packages, restores the role, and disables our accessibility service. Factory reset as the final fallback.

## Open Questions

- ~~Application ID~~ Resolved: `me.steenman.tvlauncher`.
- Does the boreal build deliver `KEYCODE_HOME` to accessibility services, or only window-state events?
- Which gradient colours? Start with a dark blue → teal and adjust on the real TV.
