## Why

There is no way to install new apps from our launcher. The stock Google TV home screen reaches the Play Store through its own Apps tab, and the Play Store has no entry of its own among the TV apps, so it never shows up under All apps. Since the stock home screen is always redirected to ours, the store is unreachable without adb.

## What Changes

- A **Play Store** button in the header, next to Settings and Screensaver.
- It opens the Play Store through the first of several intents that resolves, and hides itself when none does.
- A `<queries>` entry so the launcher can see the Play Store package and other app stores.

## Capabilities

### New Capabilities
- `app-store-shortcut`: Opening the app store from the home screen.

## Impact

- Code: new `AppStore`, header layout in `HomeScreen`, wiring in `MainActivity`, manifest `<queries>`.
- No new permissions.
