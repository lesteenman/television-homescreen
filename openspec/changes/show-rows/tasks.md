## 1. Deep-link spike (kitchen TV, when convenient)

- [ ] 1.1 (Disney+ and Spotify done; Netflix needs someone watching the TV) For Netflix, Disney+ and Spotify, open a known show with `adb shell am start -a android.intent.action.VIEW -d <link> -p <package>` from a cold start and a warm start; record in design.md D4 whether the profile picker appears and whether the app continues to the show afterwards
- [x] 1.2 Record the working link format per app in design.md D3

## 2. Config

- [x] 2.1 Decide hosting for `shows.json`: bundled in the APK for now (D1)
- [x] 2.2 Config model and parser (version check, unknown fields ignored) with unit tests
- [x] 2.3 Load `assets/shows.json` off the main thread; placeholder shows until the real list is known
- [x] 2.4 Add the `INTERNET` permission (already added by `weather-forecast`)

## 3. Artwork

- [x] 3.1 Artwork cache: download once per URL, scale to 320×180, store as WebP keyed by URL hash
- [x] 3.2 Delete artwork no longer referenced after the config is loaded
- [x] 3.3 Title-only fallback tile while artwork is missing

## 4. UI

- [x] 4.1 Show rows between Favorites and All apps: app tile first, then shows, in a `LazyRow`
- [x] 4.2 Launch shows by deep link with a fallback to the app
- [x] 4.3 Hide rows for apps that aren't installed; update when apps are installed or removed
- [x] 4.5 Leave apps with a show row out of the Favorites row, keeping their stored favorite position
- [x] 4.4 Instrumented test with a fake config (using apps present on the emulator, such as YouTube): row layout, D-pad navigation, fallback launch

## 5. Verify

- [ ] 5.1 (emulator done, see D7) Measure frame times on the emulator, then on the TV (`dumpsys gfxinfo`), and compare with design D8 of `kitchen-launcher-poc`
- [ ] 5.2 Deploy the release build to the kitchen TV with real shows; check offline behaviour
