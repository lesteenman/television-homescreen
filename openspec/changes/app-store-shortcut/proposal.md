## Why

There is no way to install new apps from our launcher. The stock Google TV home screen reaches the Play Store through its own Apps tab, and the Play Store has no entry of its own among the TV apps, so it never shows up under All apps. Since the stock home screen is always redirected to ours, the store is unreachable without adb.

## What Changes

- A **Play Store** tile at the end of All apps, with the store's own banner where it has one.
- It opens the Play Store through the first of several intents that resolves, and is left out when none does.
- A `<queries>` entry so the launcher can see the Play Store package and other app stores.

## Capabilities

### New Capabilities
- `app-store-shortcut`: Opening the app store from the home screen.

## Impact

- Code: new `AppStore`, `AppRepository.loadAppStore`, the tile in `HomeScreen`, wiring in `HomeViewModel` and `MainActivity`, manifest `<queries>`.
- No new permissions.
