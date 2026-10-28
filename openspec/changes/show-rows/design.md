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

### D4: Profiles can't be chosen from a deep link
- No app takes a profile in its link. Measured on the kitchen TV, 2026-10-28:

| App | Cold start (app not running) | Warm start (profile active) |
|---|---|---|
| Disney+ | "Who's watching?" picker; after picking a profile it **continues to the show** | Straight to the show (entity link) |
| Spotify | No profiles; **starts playing** the playlist right away | Same: starts playing |
| Netflix | Not observed yet: Netflix blocks screen capture and its UI tree is empty, so this needs someone looking at the TV | Not observed yet |

- So for Disney+ the profile picker costs one press and the link still works. Spotify links are "play this", not "show this", which suits music in the kitchen.
- Ways to reduce the friction that sit outside the launcher: a single profile per household app on this TV, or that app's own setting to skip the profile picker, where it has one.

### D5: Artwork is fetched once and cached
- Each show's `image` URL is downloaded once with `HttpURLConnection` (no new library). It is decoded and scaled to 320×180 like app banners, and stored as `files/artwork/<sha256(url)>.webp`.
- It is downloaded again only when the URL changes. Files no longer referenced in the config are deleted after the config is loaded.
- While artwork is missing or failed, the tile shows the title on the card colour.

### D6: Layout
- Order: header → Favorites → **show rows** → All apps.
- **Decided 2026-10-28:** an app with a show row is left out of the Favorites row. Its favorite entry stays stored, so it comes back in its old position if its show row is removed.
- Each show row is a full-width item in the existing grid that holds a `LazyRow`. Tile 0 is the app's banner card, which launches the app; the show cards follow, at the same 16:9 size as the other tiles. There's no row title, because the app tile already says which app it is.
- D-pad: down from Favorites lands on the row's app tile; right moves along the shows. Each row keeps its own horizontal scroll position.
- The long-press menu on a show tile is out of scope for v1. The show list is managed in the config.

### D7: Performance
- Only bitmaps at display size, the same as banners: no shaders, no autoplay, no animations beyond the standard card focus.
- Config and artwork loading happen off the main thread. The UI state for show rows only changes when the config or the artwork actually changes, keeping the "no recomposition on return home" behaviour.
- Expected memory: about 230 KB per show tile, so 30 shows is about 7 MB.

## Risks / Trade-offs

- [App deep links change or break after an app update] → Fall back to opening the app. Check the links when an app updates.
- [The profile picker eats the deep link] → The spike decides per app (D4). The worst case is the same as opening the app.
- [The config URL is down or the network is offline] → The cached config and cached artwork keep working.
- [The JSON file is public] → It holds only show titles and links. Use a secret gist URL if that matters.
- [Artwork rights] → Personal, private use on our own TV, fetched from URLs we choose.

## Open Questions

- Netflix cold and warm start behaviour (D4), which needs someone looking at the TV.
- Which shows per app to start with. v1 ships placeholders until the real list is known.
