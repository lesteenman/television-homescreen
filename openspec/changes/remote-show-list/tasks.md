## 1. Fetch and cache

- [x] 1.1 `shows.url` in `local.properties` → `BuildConfig.SHOW_LIST_URL` (empty when missing)
- [x] 1.2 `ShowConfig.remoteUrl(base, deviceName)` with the slug rule from D1, plus unit tests
- [x] 1.3 `ShowRepository`: cached copy in `files/shows.json` (`AtomicFile`), `fetch(url)` that stores only a list that parses, bundled fallback
- [x] 1.4 `HomeViewModel.keepShowsFresh()`: 15-minute refresh, 5-minute retry, prune and load artwork on change; run it from `MainActivity` while resumed

## 2. Hosting

- [ ] 2.1 Secret gist with a file per TV, starting from the bundled list (living room done; kitchen needs its device name)
- [x] 2.2 README: how to edit a TV's shows, the file naming, and what happens on a renamed TV

## 3. Verify

- [x] 3.1 Emulator: a gist edit shows up after a fetch; a broken file keeps the previous list; offline start shows the cached list
- [ ] 3.2 Kitchen TV: release build shows its gist list
- [ ] 3.3 Living room TV: release build shows its own list (when it's not in use)
