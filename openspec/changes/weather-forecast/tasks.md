## 1. Data

- [x] 1.1 Add `weatherLatitude` / `weatherLongitude` build-config fields read from `local.properties`
- [x] 1.2 Add the `INTERNET` permission
- [x] 1.3 Forecast model and Open-Meteo response parser with unit tests (including WMO code → icon group mapping)
- [x] 1.4 Weather repository: fetch, cache last success with timestamp, 30-minute refresh while resumed

## 2. UI

- [x] 2.1 Bundle vector icons for the 8 condition groups
- [x] 2.2 Weather strip in the header: today block plus 4 compact days, locale day names
- [x] 2.3 Stale label (> 3 h) and hidden state (never fetched)
- [x] 2.4 Instrumented test with a canned forecast: strip content, not focusable, hidden when there's no data

## 3. Verify

- [x] 3.1 Check the layout on the emulator at 1080p, inside the safe area
- [x] 3.2 Measure frame times (`dumpsys gfxinfo`) and compare with `kitchen-launcher-poc` design D8 (see D6)
- [x] 3.3 Deploy the release build to the kitchen TV
