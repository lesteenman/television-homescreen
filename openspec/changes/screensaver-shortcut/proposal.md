## Why

The kitchen TV often sits on the home screen when nobody is watching. The Ambient screensaver (with clock and photos) is nicer to look at, but it only starts after 10 minutes of idle, or by asking the Assistant. We want to start it on purpose with one or two presses.

## What Changes

- A **Screensaver** button in the header, next to Settings.
- **BACK on the home screen** starts the screensaver. Today BACK there does nothing.
- Both start the normal Ambient screensaver through the system's "sleep" voice action (`com.google.android.pano.action.SLEEP`), which any app may send. No root, no new permissions.
- A BACK that arrives right after returning home (for example the last of several BACK presses used to leave an app) is ignored, so the screensaver doesn't start by accident.

## Capabilities

### New Capabilities
- `screensaver-shortcut`: Starting the system screensaver from the home screen, by button and by BACK.

### Modified Capabilities
- `launcher-home-screen`: *Back and Home behaviour* changes: BACK on the home screen starts the screensaver instead of doing nothing.

## Impact

- **Depends on** `kitchen-launcher-poc` being archived first, so that `launcher-home-screen` exists as a main spec.
- Code: header layout in `HomeScreen`, the BACK callback in `MainActivity`, a `<queries>` entry for the sleep action.
- Uses an undocumented intent of Google TV's Settings app. If a system update removes it, the button hides itself and BACK goes back to doing nothing.
