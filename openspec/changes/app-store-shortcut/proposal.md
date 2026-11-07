## Why

We couldn't find a way to install new apps from our launcher. On the kitchen TV the Play Store turned out to be in All apps after all, as "Google Play Store" in the middle of the alphabetical list, where it's easy to miss. Whether it's listed at all depends on the device, since the store's TV entry isn't always a LEANBACK_LAUNCHER activity.

## What Changes

- A **Play Store** tile always at the end of All apps, with the store's own banner where it has one. The store is taken out of the alphabetical list.
- It opens the Play Store through the first of several intents that resolves, and is left out when none does.
- A `<queries>` entry so the launcher can see the Play Store package and other app stores.

## Capabilities

### New Capabilities
- `app-store-shortcut`: Opening the app store from the home screen.

## Impact

- Code: new `AppStore`, `AppRepository.loadAppStore`, the tile in `HomeScreen`, wiring in `HomeViewModel` and `MainActivity`, manifest `<queries>`.
- No new permissions.
