## Why

The kitchen TV is often on in the background, and a glance at today's weather and the next few days is more useful there than an empty header. The stock screensaver shows only the current temperature; we want the forecast on the home screen itself.

## What Changes

- A **weather strip** at the top of the home screen, between the Settings button and the clock:
  - Today: condition icon, current temperature, today's high and low, and chance of rain.
  - The next 4 days: day name, icon, high and low.
- Data comes from **Open-Meteo**, a free forecast API that needs no API key, for a fixed location set at build time.
- It refreshes every 30 minutes while the launcher is in the foreground, and the last result is cached so it shows immediately and offline. Stale data is labelled with its age, and with no data yet the strip is hidden.
- The weather icons are vector icons bundled in the app, with no animation. This keeps the GPU cost of the home screen low.
- The launcher gets the `INTERNET` permission, and the "nothing from the network" rule is relaxed to allow the weather forecast.

## Capabilities

### New Capabilities
- `weather-forecast`: Fetching, caching and refreshing the forecast, and the weather strip's content and behaviour when data is stale or missing.

### Modified Capabilities
- `launcher-home-screen`: The "no network" part of *Gradient background with no ads or recommendations* changes to allow the forecast, and the header layout gains the weather strip.

## Impact

- **Depends on** `kitchen-launcher-poc` being archived first, so that `launcher-home-screen` exists as a main spec.
- Code: a new weather data layer, a header layout change in `HomeScreen`, the `INTERNET` permission, and new vector icons.
- External service: `api.open-meteo.com`. It's free for non-commercial use, and one small request every 30 minutes is far below its limits.
- `show-rows` modifies the same network requirement, so whichever change is archived second must merge the wording.
