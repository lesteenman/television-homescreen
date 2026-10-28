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

### D1: Shows come from a remote JSON file
- At startup, and on resume when the cached copy is older than 6 hours, the launcher fetches a JSON file from a URL fixed at build time (`showsConfigUrl` from `gradle.properties` or `local.properties`). The last good copy is cached in app storage and used offline and when a fetch fails.
- Adding a show means editing that file. The TV picks it up within 6 hours, or straight away after a launcher restart.
- Alternatives considered:
  - **Hard-coded in the APK:** simplest, but every change means a rebuild and a deploy over adb, with wireless debugging switched on by hand.
  - **Editing on the TV:** entering a deep link or show ID with the remote's on-screen keyboard is painful, and other apps don't share show links on TV.
  - **A small web page served by the launcher on the LAN:** pleasant to use from a phone, but it means running an HTTP server on the TV, with security and complexity costs. Worth keeping in mind for later.
- Hosting options: `https://steenman.me/tvlauncher/shows.json`, or a secret GitHub gist (raw URL). It must be HTTPS; the file is public but unguessable at most. It holds nothing sensitive, just show titles and links.

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
- Expected link formats, to be confirmed in the spike:
  - Netflix: `https://www.netflix.com/title/<id>`
  - Disney+: `https://www.disneyplus.com/series/<slug>/<id>`
  - Spotify: `spotify:playlist:<id>`, `spotify:album:<id>`, `spotify:show:<id>`

### D4: Profiles can't be chosen from a deep link
- As far as public behaviour goes, Netflix and Disney+ deep links carry no profile parameter.
  - **Cold start** (app not running): expect the profile picker first. Whether the app then continues to the show or drops you on its home page differs per app.
  - **Warm start** (a profile already active): the link should go straight to the show.
  - Spotify has no profiles, so its links go straight through.
- The spike (tasks 1.x) records the actual behaviour per app on the kitchen TV. If an app forgets the link after the profile picker, its tiles can open the app and show a hint instead.
- Ways to reduce the friction that sit outside the launcher: a single profile per household app on this TV, or that app's own setting to skip the profile picker, where it has one.

### D5: Artwork is fetched once and cached
- Each show's `image` URL is downloaded once with `HttpURLConnection` (no new library). It is decoded and scaled to 320×180 like app banners, and stored as `files/artwork/<sha256(url)>.webp`.
- It is downloaded again only when the URL changes. Files no longer referenced in the config are deleted after a successful config fetch.
- While artwork is missing or failed, the tile shows the title on the card colour.

### D6: Layout
- Order: header → Favorites → **show rows** → All apps.
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

- Where do we host `shows.json`: steenman.me or a secret gist?
- Which shows per app to start with?
- Should apps with a show row still appear in Favorites, or does the show row replace their favorite tile?
