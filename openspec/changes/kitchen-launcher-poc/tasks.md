## 1. Project & dev loop setup

- [x] 1.1 Decide application ID: `me.steenman.tvlauncher` (recorded in design.md)
- [x] 1.2 Scaffold Gradle Android project (Kotlin, Compose for TV, minSdk/targetSdk 34) with Gradle wrapper; `./gradlew assembleDebug` succeeds
- [x] 1.3 Download `system-images;android-34;google-tv;arm64-v8a` (confirm with user first) and create AVD `gtv-34` via `scripts/emulator.sh`
- [x] 1.4 Write `scripts/device.sh` that resolves the kitchen connect port from `adb mdns services`, runs `adb connect`, and fails clearly when no device is advertised
- [x] 1.5 Write `scripts/deploy.sh` (emulator or device target) that runs `installDebug` against the chosen target

## 2. Launcher home screen

- [x] 2.1 Manifest: HOME activity with MAIN + HOME + DEFAULT + LEANBACK_LAUNCHER, `<queries>` for leanback launcher intents
- [x] 2.2 App repository: query leanback launcher activities, load banner/icon/label, exclude self
- [x] 2.3 Package change receiver + `onResume` refresh so installs/uninstalls update the grid
- [x] 2.4 Gradient background and overscan-safe layout scaffold (48 dp / 27 dp margins)
- [x] 2.5 Clock header updated on `ACTION_TIME_TICK`, honouring 12/24-hour setting
- [x] 2.6 "All apps" grid of 16:9 focusable cards; OK launches the app
- [x] 2.7 Favorites store in DataStore (ordered component names); prune uninstalled apps
- [x] 2.8 Favorites row(s) above the grid, hidden when empty
- [x] 2.9 Long-press action sheet: add/remove favorite, move left/right, app info
- [x] 2.10 Settings tile opening `Settings.ACTION_SETTINGS`
- [x] 2.11 Initial focus, BACK does nothing, HOME-while-home scrolls to top and resets focus
- [x] 2.12 Verify all launcher-home-screen scenarios in the emulator

## 3. Kitchen device prep

- [x] 3.1 Uninstall Twilight: `pm uninstall --user 0 com.urbandroid.lux`; verify package and accessibility entry are gone
- [x] 3.2 Record a baseline of the device (HOME handlers, role holders `cmd role get-role-holders android.app.role.HOME`, disabled packages, accessibility settings) to `docs/kitchen-baseline.md`
- [x] 3.3 Install the launcher on the device and verify it launches manually and lists apps with banners
- [x] 3.4 Verify wireless debugging is still reachable after a reboot (mDNS port rediscovered) — finding: wireless debugging turns OFF on reboot and must be re-enabled by hand; pairing persists and `device.sh` rediscovers the new port

## 4. HOME takeover spike

- [x] 4.1 Write `scripts/restore-stock.sh` (enable launcherx + setupwraith, HOME role back to launcherx, disable our accessibility service); test it on the unmodified device
- [x] 4.2 Approach E: `scripts/takeover-e.sh` assigning the HOME role; test HOME ×10, reboot, record result in design.md — result: FAILS (priority-2 launcherx wins over the role-preferred activity)
- [x] 4.3 If E holds, run the 24-hour stickiness check and stop the spike here — n/a, E failed
- [x] 4.4 If E fails: restore-stock, then approach B `scripts/takeover-b.sh` disabling launcherx; observe whether setupwraith RecoveryActivity wins — result: RecoveryActivity wins HOME; HOME from an app does nothing visible
- [x] 4.5 Only if RecoveryActivity wins: extend B to disable setupwraith; retest HOME ×10 and reboot — HOME 10/10 with launcherx + setupwraith disabled; after reboot the TV boots straight into our launcher (2026-10-28)
- [x] 4.6 Check voice button, Settings access and screensaver still work under B; record findings — voice search, Assistant, Settings OK; screensaver configured but not observed
- [x] 4.7 If B is unstable: implement accessibility watchdog service (window-state events for launcherx HomeActivity → start our launcher), `scripts/takeover-c.sh` to enable it — n/a so far: B is stable through 10 HOME presses and a reboot
- [ ] 4.8 Run 24-hour stickiness check for the chosen approach and document the final state and undo in design.md
