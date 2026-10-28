## Context

`kitchen-launcher-poc` applied approach B on the kitchen Chromecast HD: launcherx and setupwraith disabled, our app holding the HOME role. With launcherx gone, other apps failed to reach its content providers (`AccountModeContentProvider`, `UserModeContentProvider`, `sliceprovider`). Ambient (`dreamx`) then reported "account mode is not valid" and "Getting device settings needs cast device ID, which is unavailable", hid Google Photos, and the device vanished from Google Home.

## Goals / Non-Goals

**Goals:**
- Our launcher stays the home screen, while launcherx's services work for the rest of the system.

**Non-Goals:**
- Removing the brief flash of the stock home screen.

## Decisions

### D1: Narrower B is not possible
- Disabling only launcherx's `.home.HomeActivity` / `.home.VanillaModeHomeActivity` and setupwraith's `.MainActivity` / `.RecoveryActivity` would have kept everything else. `pm disable` on a component fails with `SecurityException: Shell cannot change component state … to 2`: shell may only switch whole packages of non-test apps (2026-10-28).

### D2: Approach C, window watcher only
- `HomeRedirectService` listens only to `TYPE_WINDOW_STATE_CHANGED` from `com.google.android.apps.tv.launcherx`, with `canRetrieveWindowContent=false`. When the class starts with `com.google.android.apps.tv.launcherx.home.`, it starts our `MainActivity` with a HOME intent (so the launcher resets to the top). Accessibility services may start activities from the background.
- Key filtering for HOME was tried as well: HOME sent with `adb shell input` never reached the service. It was removed, since it would also let the service see every key press. Whether a real remote's HOME would reach it wasn't tested.
- `scripts/takeover-c.sh` enables the service before re-enabling the stock packages, so the redirect is already active when launcherx returns.

### D3: Measurements (kitchen TV, 2026-10-28)
- HOME from Settings, 3 runs: launcherx resumed, then ours 147–308 ms later (`wm_resume_activity` events). Within the spec's 500 ms.
- HOME from Settings and BACK out of Settings: our launcher on screen every time.
- Ambient restarted with C applied: no "account mode is not valid", no provider lookup failures anywhere in the log. Google Photos selectable again in Ambient; the TV visible again in Google Home (checked by the user).

## Risks / Trade-offs

- [The accessibility service is killed or switched off] → HOME shows the stock home screen; nothing breaks. Re-run `scripts/takeover-c.sh`.
- [launcherx renames its home activity] → The class prefix no longer matches and the stock screen stays. Check after launcherx updates.
- [Stock launcher runs in the background] → Its recommendations exist but are only on screen for a fraction of a second.

- After the TV restarted (wireless debugging went off, as it does on every boot), it showed our launcher (checked by the user).

## Open Questions

- None.
