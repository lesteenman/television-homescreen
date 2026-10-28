## Why

The home screen only opens apps, so getting to the show you're watching means opening the app and finding it there. We want one-press access to a handful of chosen shows, without bringing back Google's recommendations. Other apps' "Watch Next" and preview rows can't be read: that needs the system-only `ACCESS_ALL_EPG_DATA` permission (checked on the emulator, 2026-10-28). So the shows come from a list we write ourselves.

## What Changes

- New **show rows**: one horizontal row per app that has configured shows. The leftmost tile is the app itself, with the show tiles to its right. The rows sit between Favorites and All apps.
- Selecting a show tile opens that show in its app through a **deep link**. If the link can't be opened, the app opens instead.
- The shows are defined in a **JSON file bundled in the APK** for now. Changing them needs a rebuild and deploy; the format is ready for a remote file later, so shows can then change without one.
- An app with a show row is **left out of the Favorites row**, so each app appears once above All apps.
- **Artwork** for each show is downloaded once from the URL in the config, cached on the device, and only downloaded again if its URL changes.
- The launcher gets the `INTERNET` permission. The "nothing from the network" rule is relaxed to allow user-configured content only. There are still no recommendations or ads.
- A spike on the kitchen TV checks how deep links behave per app: Netflix, Disney+, Spotify, and in particular profile selection.

## Capabilities

### New Capabilities
- `show-rows`: The show configuration source and caching, the per-app show rows, deep-link launching with a fallback, and artwork fetching and caching.

### Modified Capabilities
- `launcher-home-screen`: The "no network" part of *Gradient background with no ads or recommendations* changes to allow user-configured content, and the layout gains the show rows between Favorites and All apps.

## Impact

- **Depends on** `kitchen-launcher-poc` being archived first, so that `launcher-home-screen` exists as a main spec to modify.
- Code: a new data layer (config parse, artwork cache), new UI rows in `HomeScreen`, and the `INTERNET` permission in the manifest.
- Hosting: none for the show list in v1. The artwork comes from image URLs chosen per show.
- `weather-forecast` modifies the same network requirement, so whichever change is archived second must merge the wording.
