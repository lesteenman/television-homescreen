## Context

The launcher (see `kitchen-launcher-poc`) shows Favorites and All apps as 16:9 banner tiles in one `LazyVerticalGrid` on a Chromecast HD (Android 14). Performance work there established two things: drawing bitmaps is cheap on this GPU, and live shaders are expensive. App entries are cached so that returning home doesn't recompose.

Other apps' recommendations are not available to us. `READ_EPG_DATA` is a normal permission, but it only exposes an app's own preview programs. Reading other apps' rows needs `ACCESS_ALL_EPG_DATA` or `ACCESS_WATCHED_PROGRAMS`, and both are `signature|privileged`. Google TV's "For you" rows mostly come from Google's servers anyway.

## Goals / Non-Goals

**Goals:**
- One press from home to a specific show in Netflix, Disney+, Spotify, and any app with deep links.
- Add, remove or reorder shows without rebuilding or reinstalling the launcher.
- Keep the home screen fast and usable offline.

**Non-Goals:**
- Automatic "continue watching" or progress tracking.
- Choosing an app profile from the launcher (see D4).
- Editing the show list on the TV itself in v1. See D1 for why.

## Decisions

### D1: Shows come from a bundled JSON file (for now)
- **Decided 2026-10-28:** v1 bundles `app/src/main/assets/shows.json` in the APK. Adding a show means editing that file, rebuilding, and deploying with `scripts/deploy.sh tv --release`.
- The parser takes a string and doesn't care where it came from, so a remote source can be added later without changing the format.
- Alternatives, kept for later:
  - **Remote file** (secret GitHub gist or `https://steenman.me/tvlauncher/shows.json`): changes without a rebuild. It would add fetching, caching and a refresh policy (at start, and on resume when older than 6 hours).
  - **Editing on the TV:** entering a deep link or show ID with the remote's on-screen keyboard is painful, and other apps don't share show links on TV.
  - **A small web page served by the launcher on the LAN:** pleasant to use from a phone, but it means running an HTTP server on the TV, with security and complexity costs.

### D2: Config format

```json
{
  "version": 1,
  "rows": [
    {
      "app": "com.netflix.ninja",
      "shows": [
        { "title": "Example show", "link": "https://www.netflix.com/title/80000000", "image": "https://…/example.jpg" }
      ]
    }
  ]
}
```

- Rows and shows appear in file order. A row whose `app` isn't installed is hidden.
- Unknown fields are ignored, so the format can grow. A file with a different `version` is rejected, and the cached copy stays in use.

### D3: Deep-link launching
- Launch with `Intent(ACTION_VIEW, Uri.parse(link)).setPackage(app)` and `FLAG_ACTIVITY_NEW_TASK`. If nothing resolves it, fall back to the app's leanback launch intent.
- Link formats, from the apps' intent filters on the kitchen TV (2026-10-28):
  - Netflix (`com.netflix.ninja`): `https://www.netflix.com/title/<id>` (also `/watch/<id>`, and the `nflx://` scheme).
  - Disney+ (`com.disney.disneyplus`): any `https://www.disneyplus.com/...` URL resolves, but only the current form opens the show: `https://www.disneyplus.com/browse/entity-<uuid>`. The old `/series/<slug>/<id>` form lands on the Disney+ home page. Copy links from the website's address bar after it has redirected.
  - Spotify (`com.spotify.tv.android`): `spotify:<type>:<id>` or `https://open.spotify.com/<type>/<id>` for album, artist, playlist, show and more.
  - Videoland (`nl.rtl.videoland.v2`, confirmed on the TV): `https://v2.videoland.com/<slug>-p_<id>`, handled by its `DeepLinkActivity` (path glob `/.*_.*`). There is also a `videoland-v2://` scheme.

### D3a: Where the show list comes from (2026-10-28)
- Links: JustWatch's Dutch pages (`justwatch.com/nl/tv-series/<slug>`) link to each service's own page, which gives Netflix title IDs, Disney+ entity links and Videoland page links for the Dutch catalogue. All 9 were opened on the TV. JustWatch's Bluey link was wrong (it is "Bluey Compilations"); the main series was found by reading the `og:title` of the Disney+ pages linked from that page.
- Artwork: TMDB backdrops at `https://image.tmdb.org/t/p/w780/<file>.jpg`, picked by hand from the show's backdrops page (`themoviedb.org/tv/<id>/images/backdrops`). Versions with the show's title in the image are preferred, so they're easy to recognise. The first image on that page is the show's poster (2:3), not a backdrop: check that a picked image is 16:9, or the tile crops it to its middle.

### D4: Profiles can't be chosen from a deep link
- No app takes a profile in its link. Measured on the kitchen TV, 2026-10-28:

| App | Cold start (app not running) | Warm start (profile active) |
|---|---|---|
| Disney+ | "Who's watching?" picker; after picking a profile it **continues to the show** | Straight to the show (entity link) |
| Spotify | No profiles; **starts playing** the playlist right away | Same: starts playing |
| Netflix, plain `ACTION_VIEW` | Picker, then the Netflix **home page**: the link is lost | Netflix comes to the front where it was; the link is **ignored** |
| Netflix, partner launch (below) | **Straight to the show**, no picker (picker behaviour may depend on how recently a profile was chosen) | Straight to the show |
| Videoland | "Wie kijkt er?" picker; after picking a profile it **continues to the show** | Straight to the show |

- Netflix partner launch: the same `ACTION_VIEW` link plus `FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS` and the extra `source=30`, as TV partner apps launch Netflix. `showIntent()` adds this for `com.netflix.ninja` only. Tested on the kitchen TV, 2026-10-28, from the launcher's own tile too.
- No app has a link or URL parameter that picks a profile. Videoland's `/profielkeuze` path only opens its picker.
- So for Disney+ the profile picker costs one press and the link still works. Spotify links are "play this", not "show this", which suits music in the kitchen.
- Ways to reduce the friction that sit outside the launcher: a single profile per household app on this TV, or that app's own setting to skip the profile picker, where it has one.

### D5: Artwork is fetched once and cached
- Each show's `image` URL is downloaded once with `HttpURLConnection` (no new library). It is decoded and scaled to 320×180 like app banners, and stored as `files/artwork/<sha256(url)>.webp`.
- It is downloaded again only when the URL changes. Files no longer referenced in the config are deleted after the config is loaded.
- While artwork is missing or failed, the tile shows the title on the card colour.

### D6: Layout
- Order: header → **show rows** → Favorites → All apps. **Decided 2026-10-28:** apps with shows come first, in config order; the remaining favorites follow under the Favorites title. Initial focus is the first row's app tile.
- **Decided 2026-10-28:** an app with a show row is left out of the Favorites row. Its favorite entry stays stored, so it comes back in its old position if its show row is removed.
- Each show row is a full-width item in the existing grid that holds a `LazyRow`. Tile 0 is the app's banner card, which launches the app; the show cards follow, at the same 16:9 size as the other tiles. There's no row title, because the app tile already says which app it is.
- D-pad: down moves from row to row, starting on each row's app tile; right moves along the shows. Each row keeps its own horizontal scroll position.
- The long-press menu on a show tile is out of scope for v1. The show list is managed in the config.

### D7: Performance
- Only bitmaps at display size, the same as banners: no shaders, no autoplay, no animations beyond the standard card focus.
- Config and artwork loading happen off the main thread. The UI state for show rows only changes when the config or the artwork actually changes, keeping the "no recomposition on return home" behaviour.
- Expected memory: about 230 KB per show tile, so 30 shows is about 7 MB.
- Emulator A/B, 2026-10-28: the same release build without show rows and with a YouTube row of 6 shows, 3 runs each of 3 HOME returns plus a D-pad tour. Without: median 17 ms, p95 19 ms, janky 3.0–3.5%. With: median 17 ms, p95 18–19 ms, janky 2.4–2.8%. No measurable difference.
- Kitchen TV, 2026-10-28, release build with the 3 real rows, same method as D8, 3 runs: median 23 ms, p95 28–38 ms, p99 69–85 ms, janky 4.6–5.6%. The median matches the weather-forecast run (24 ms), but the tail is a bit higher (p99 there 48–61 ms, janky 4.2–4.7%). The same key presses now move through the rows, so it isn't a like-for-like comparison. It doesn't feel slower in use; a Baseline Profile for our own code (D8 of `kitchen-launcher-poc`) is the next step if it ever does.

### D8: Implementation notes
- `app/src/debug/assets/shows.json` overrides the real list in debug builds with a YouTube row (artwork, a title-only tile, a link YouTube can't handle, and enough shows to scroll), so the emulator, which has no Netflix, Disney+ or Spotify, has something to show. Release builds use `app/src/main/assets/shows.json`.
- Each row is a `LazyRow` inside a full-width grid item, with tiles exactly as wide as a grid column. It reaches into the grid's side margins, so a focused tile scrolled to the edge isn't clipped.
- `showIntent()` resolves the deep link against the row's package first; when nothing in the app handles it, the tile opens the app.
- The long-press menu on a row's app tile works as on other tiles; "Remove from favorites" there removes the hidden favorite.

## Risks / Trade-offs

- [App deep links change or break after an app update] → Fall back to opening the app. Check the links when an app updates.
- [The profile picker eats the deep link] → The spike decides per app (D4). The worst case is the same as opening the app.
- [The config URL is down or the network is offline] → The cached config and cached artwork keep working.
- [The JSON file is public] → It holds only show titles and links. Use a secret gist URL if that matters.
- [Artwork rights] → Personal, private use on our own TV, fetched from URLs we choose.

## Open Questions

- Skipping the profile pickers isn't possible through links (D4).
