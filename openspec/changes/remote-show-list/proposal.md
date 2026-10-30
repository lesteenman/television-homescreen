## Why

The show list is bundled in the APK, so adding or removing a show means a rebuild and an install over wireless adb, which is off after every TV reboot. The two TVs also want different shows. Each TV should get its list from a file that can be edited on github.com, without touching the app.

## What Changes

- The launcher fetches its show list from a **secret GitHub gist**: one file per TV, named after the TV's device name (for example `living-room-tv-2.json`), in the existing `shows.json` format.
- It fetches the file on start and again at most every 15 minutes while the home screen is open, and keeps the last valid copy on the device.
- If the file can't be fetched, is missing or is invalid, the launcher keeps showing the last valid copy; before there is one, it shows the bundled list.
- The gist address comes from `local.properties` (`shows.url`), like the weather location, so it never appears in the public repo. Without it, the launcher uses only the bundled list, as before.

## Capabilities

### New Capabilities

None.

### Modified Capabilities
- `show-rows`: *Show configuration bundled with the app* becomes a remote list per TV, with the bundled file as the fallback.

## Impact

- **Depends on** `show-rows` being archived first, so that `show-rows` exists as a main spec to modify.
- Code: `ShowRepository` gains fetch and cache, `HomeViewModel` a refresh loop like the weather's, and `MainActivity` runs it while resumed. A new `shows.url` build setting.
- Hosting: a secret gist on the owner's GitHub account. Anyone with the link can read it; it holds only show names, links and image URLs.
