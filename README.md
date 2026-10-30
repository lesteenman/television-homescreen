# Television homescreen

A replacement home screen for a Chromecast with Google TV: no ads and no recommendations, just the apps and shows we actually use, the time and the weather. It runs on a Chromecast HD in the kitchen and a Google TV Streamer in the living room, both on Android 14.

![The home screen on the Google TV emulator](docs/screenshot.png)

*On the emulator, with the debug build's demo row of Blender open movies. On the TV, rows hold the shows we picked for Netflix, Disney+ and Videoland.*

## What it does

- **Show rows.** One row per app with picked shows: the app's tile first, then its shows. Selecting a show opens it in the app by deep link. Each TV fetches its own list from a secret gist, so shows change without an app update (see below); [`shows.json`](app/src/main/assets/shows.json) in the app is the fallback until the first fetch. Artwork is downloaded once and cached.
- **Favorites and all apps.** Favorites can be added, removed and reordered with a long press on OK. Apps that have a show row aren't repeated in Favorites.
- **Weather.** Today and the next four days from [Open-Meteo](https://open-meteo.com/), refreshed every 30 minutes and cached for offline use.
- **Screensaver.** The header's Screensaver button, or BACK on the home screen, starts the normal Ambient screensaver.
- **Fast on old hardware.** No live shaders; the background gradient is pre-rendered.

## How it replaces the stock home screen

Google TV's own launcher can't be outranked as the HOME app, and disabling it breaks things that depend on it, such as Google Photos in Ambient and the TV's entry in Google Home. So the stock launcher stays enabled, and an accessibility service in this app ([`HomeRedirectService`](app/src/main/java/me/steenman/tvlauncher/HomeRedirectService.kt)) brings this launcher up whenever the stock home screen appears. The service only sees which app opened a window; it doesn't read the screen or key presses.

The full story, including the approaches that didn't work, is in [`openspec/changes/`](openspec/changes/), in particular `kitchen-launcher-poc` and `takeover-redirect`.

## Building

Requirements: JDK 17+, the Android SDK (platform 37), and for the emulator the `google-tv` Android 34 system image.

Optional settings in `local.properties` (git-ignored):

```properties
# Weather location; without these, the weather strip stays hidden.
weather.latitude=52.10
weather.longitude=5.18

# Raw base URL of the show-list gist; without it, only the bundled shows.json is used.
shows.url=https://gist.githubusercontent.com/<user>/<gist-id>/raw/
```

### Changing the shows

Each TV reads `<device name>.json` from the gist: the TV's name (Settings → System → About) in lower case, with spaces and other characters turned into `-`. "Living Room TV 2" reads `living-room-tv-2.json`. The format is the same as [`shows.json`](app/src/main/assets/shows.json).

Edit the file on gist.github.com. The TV picks it up the next time the home screen opens at least 15 minutes after its last check; GitHub can add a few minutes of caching. A file that is missing or doesn't parse is ignored, and the TV keeps the last list it read (`adb logcat -s TvLauncher` shows why). Renaming a TV means renaming its file.

Release builds are signed with a local key described by `keystore.properties` (git-ignored); see [`docs/release-signing.md`](docs/release-signing.md).

```bash
./gradlew testDebugUnitTest
```

## Scripts

All in [`scripts/`](scripts/), run from the repository root:

| Script | What it does |
|---|---|
| `emulator.sh` | Creates (once) and boots the `gtv-34` Google TV emulator |
| `deploy.sh [tv\|emulator] [--release]` | Builds, installs and starts the launcher |
| `device.sh` | Connects to the TV over wireless debugging, finding its port via mDNS |
| `takeover-c.sh` | Makes this the home screen on the TV (the approach in use) |
| `restore-stock.sh` | Undoes every takeover change and returns the TV to the stock launcher |

With one TV on the network, the scripts find it on their own. With several, name it: `TV=living-room scripts/deploy.sh tv --release`. The names are mapped to the TVs' mDNS names in `scripts/local.env` (git-ignored, since those names contain serial numbers):

```bash
TV_MDNS_KITCHEN=adb-<serial>
TV_MDNS_LIVING_ROOM=adb-<serial>
```

To pair a new TV, open Settings → System → Developer options → Wireless debugging → Pair device with pairing code on it, then run `adb pair <ip>:<port> <code>` with what it shows. Wireless debugging switches itself off on every reboot and has to be switched on again on the TV.

## Development

Changes are planned with [OpenSpec](https://github.com/Fission-AI/OpenSpec): each folder in `openspec/changes/` has a proposal, design, specs and tasks. On-device tests run on the emulator:

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest
```
