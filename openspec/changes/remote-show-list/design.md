## Context

`ShowRepository.loadConfig()` reads `assets/shows.json` once at start. The parser (`ShowConfig.parse`) doesn't care where the text comes from. Artwork is cached by URL and pruned when a URL leaves the config, so a list that changes at runtime needs no artwork changes. The weather already has the pattern this needs: a cached copy on disk, read at start, and a refresh loop that runs while the activity is resumed.

## Goals / Non-Goals

**Goals:**
- Change a TV's shows by editing a file on github.com, with no build or adb.
- A different list per TV, from the same APK.
- The home screen never gets worse because of the remote file: offline, missing or broken means "keep what you had".

**Non-Goals:**
- Editing shows on the TV itself.
- Private hosting with credentials on the device.
- Changing the file format.

## Decisions

### D1. One secret gist, one file per TV
The gist holds `<device-name-slug>.json` per TV. The launcher builds the URL as `<shows.url><slug>.json`, where `shows.url` is the gist's raw base (`https://gist.githubusercontent.com/<user>/<id>/raw/`) and the slug is the device name lowercased, with every run of other characters than `a-z0-9` turned into `-` ("Living Room TV 2" → `living-room-tv-2`). A raw URL without a revision always serves the latest version, possibly up to about 5 minutes old because of GitHub's cache.

*Alternatives:* one file with a map of TVs (one fetch, but every edit touches both TVs' lists); a URL baked in per TV at build time (needs a separate build per TV); a setting sent over adb at install (fails whenever wireless debugging is off).

*Device name:* `Settings.Global.DEVICE_NAME`, which is what the TV shows under Settings > System > About and what Google Home calls it; `Build.MODEL` if it's empty. Renaming a TV changes its file name; until a file with the new name exists it keeps its last list.

### D2. Last valid copy wins
A fetched file is parsed before it's stored, so only a valid list replaces the cached one (`files/shows.json`, an `AtomicFile`). At start the launcher shows the cached copy, or the bundled list when there is none. A failed fetch (offline, 404, invalid JSON, unsupported version) is logged and changes nothing. An empty `rows` array is valid and removes all show rows.

### D3. Refresh while the home screen is visible
`HomeViewModel.keepShowsFresh()` runs in `repeatOnLifecycle(RESUMED)` next to the weather. It fetches on the first resume after start, then every 15 minutes while resumed; returning home within 15 minutes of a fetch doesn't fetch again. A failed fetch is retried after 5 minutes. When the list changes, artwork that is no longer used is pruned and new artwork is loaded.

### D4. No address, no fetch
With `shows.url` missing from `local.properties`, `BuildConfig.SHOW_LIST_URL` is empty and the launcher behaves exactly as before: bundled list only.

## Risks / Trade-offs

- [Anyone with the gist link can read the lists] → They contain only show titles, public links and public image URLs. The link isn't in the public repo.
- [A wrong edit hides the shows] → An invalid file is ignored and the previous list stays; the log names the error.
- [GitHub's raw cache delays an edit by a few minutes] → Acceptable for this use.
- [Renaming a TV loses its list file] → It keeps its last list; documented in the README.
