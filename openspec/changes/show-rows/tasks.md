## 1. Deep-link spike (kitchen TV, when convenient)

- [ ] 1.1 For Netflix, Disney+ and Spotify, open a known show with `adb shell am start -a android.intent.action.VIEW -d <link> -p <package>` from a cold start and a warm start; record in design.md D4 whether the profile picker appears and whether the app continues to the show afterwards
- [ ] 1.2 Record the working link format per app in design.md D3

## 2. Config

- [ ] 2.1 Decide hosting for `shows.json` (steenman.me or secret gist) and add `showsConfigUrl` as a build-config field
- [ ] 2.2 Config model and parser (version check, unknown fields ignored) with unit tests
- [ ] 2.3 Config repository: fetch over HTTPS, cache last valid copy, refresh at start and on resume when older than 6 hours
- [ ] 2.4 Add the `INTERNET` permission

## 3. Artwork

- [ ] 3.1 Artwork cache: download once per URL, scale to 320×180, store as WebP keyed by URL hash
- [ ] 3.2 Delete artwork no longer referenced after a successful config fetch
- [ ] 3.3 Title-only fallback tile while artwork is missing

## 4. UI

- [ ] 4.1 Show rows between Favorites and All apps: app tile first, then shows, in a `LazyRow`
- [ ] 4.2 Launch shows by deep link with a fallback to the app
- [ ] 4.3 Hide rows for apps that aren't installed; update when apps are installed or removed
- [ ] 4.4 Instrumented test with a fake config (using apps present on the emulator, such as YouTube): row layout, D-pad navigation, fallback launch

## 5. Verify

- [ ] 5.1 Measure frame times on the emulator, then on the TV (`dumpsys gfxinfo`), and compare with design D8 of `kitchen-launcher-poc`
- [ ] 5.2 Deploy the release build to the kitchen TV with real shows; check offline behaviour
