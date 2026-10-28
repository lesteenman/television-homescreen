## Context

The header currently holds a Settings button on the left and the clock and date on the right, with empty space between them. That space is the natural spot for weather. The device is the kitchen Chromecast HD: the GPU is weak (live shaders hurt), drawing bitmaps and vectors is cheap, and the screen should look calm.

## Goals / Non-Goals

**Goals:**
- See at a glance: current conditions, today's range, chance of rain, and the next 4 days.
- No API key or account; works offline using the last fetched data.
- No measurable effect on frame times.

**Non-Goals:**
- Hourly charts, radar, weather alerts, or multiple locations.
- Detecting the location automatically. A Chromecast has no GPS, and IP geolocation would need yet another service.

## Decisions

### D1: Open-Meteo as the data source
- One request per refresh:
  `https://api.open-meteo.com/v1/forecast?latitude=<lat>&longitude=<lon>&current=temperature_2m,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto&forecast_days=5`
- Alternatives: OpenWeatherMap and WeatherAPI both need API keys. KNMI (the Dutch weather service) offers open data but no simple forecast endpoint. Open-Meteo needs no key and is accurate for the Netherlands, because it uses KNMI's HARMONIE model among others.

### D2: Location is fixed at build time
- `weatherLatitude` and `weatherLongitude` are build-config fields, read from `local.properties` so the home location stays out of git.
- If `show-rows` lands first, the location could move into its remote config file. That's left open here.
- Without both properties, `BuildConfig` gets `NaN`, no request is ever made, and the header looks as it did before.
- For now `local.properties` holds De Bilt (52.10, 5.18) as a placeholder until the real location is chosen.

### D3: Refresh and caching
- Fetch on start, then every 30 minutes while the launcher is resumed. Coming back home triggers a fetch only if the last one is older than 30 minutes.
- Store the last successful response as JSON in app storage, with its fetch time. Show it straight away on start.
- If the data is older than 3 hours, show "updated HH:mm" in the secondary text colour. If there has never been a successful fetch, hide the strip; the header then looks as it does today.

### D4: Layout

```
[Settings]   ☁ 12°  ↑14° ↓7°  💧40%  │  Thu ☀ 15°/6°  Fri 🌧 11°/5°  Sat ⛅ 13°/6°  Sun ☁ 12°/7°      12:34
                                                                                          Wed 28 October
```

- Today's block is slightly larger; the 4 days are compact columns. Everything stays inside the existing overscan-safe margins.
- Day names follow the device locale. Temperatures are rounded to whole °C.
- The strip is not focusable. It's for reading only, so D-pad navigation doesn't change.

### D5: Icons
- WMO weather codes are mapped to about 8 groups: clear, partly cloudy, cloudy, fog, drizzle, rain, snow, thunderstorm. Each group has a bundled vector icon, with day versions only in v1.
- No animated icons, and no images from the network.

### D6: Performance
- The strip is text plus a few small vectors, recomposed only when new data arrives, which is at most every 30 minutes.
- Re-measure frame times with `dumpsys gfxinfo` after implementation, and compare with `kitchen-launcher-poc` design D8.
- Emulator A/B, 2026-10-28: the same release build with and without coordinates (strip hidden vs shown), 3 runs each of 3 HOME returns plus a D-pad tour. Without: median 17 ms, p95 17–19 ms, janky 3.0–3.5%. With: median 16–17 ms, p95 17–20 ms, janky 3.1–3.5%. No measurable difference.
- Kitchen TV, 2026-10-28, release build with the strip, same method as D8, 3 runs: median 24 ms, p95 26–34 ms, p99 48–61 ms, janky 4.2–4.7%. That matches D8's last row (24 / 34 / 48 ms, 4%), so the strip costs nothing noticeable on the Chromecast HD.

### D7: Implementation notes
- The fetch uses `HttpURLConnection` and the platform's `org.json`, so no networking or JSON library is added.
- The cache is the raw response plus its fetch time in one `AtomicFile`; it goes through the same parser as a fresh response.
- Six days are requested, so a forecast cached overnight still has 4 days after today. Days before today are skipped when drawing, and the strip hides itself once the cache has no entry for today.
- The icons are `ImageVector`s built in code (`WeatherIcons`), which is the same as bundled vector drawables at runtime but easier to compose from shared shapes.
- The refresh loop runs in `repeatOnLifecycle(RESUMED)`. It waits until 30 minutes after the last fetch, and retries after 5 minutes when a fetch fails.

## Risks / Trade-offs

- [Open-Meteo unavailable or rate-limited] → The cached forecast keeps showing, labelled with its age.
- [The header gets crowded at 1080p] → The layout sketch fits in the 864 dp-wide safe area. If it doesn't, drop chance of rain or go to 3 days.
- [The location is stored in the APK] → It's only coordinates for weather. They stay out of git via `local.properties`.

## Open Questions

- Which location: home coordinates, or a nearby town centre?
- 4 days after today, or a full week in a smaller font?
- Should tapping or selecting the strip ever show more detail, such as hourly? That's out of scope for v1.
