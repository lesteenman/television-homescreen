## ADDED Requirements

### Requirement: Show configuration from a remote file
The launcher SHALL read its show rows from a JSON file at a build-time configured HTTPS URL, using the format in design.md (D2). It SHALL fetch the file at startup, and on resume when the cached copy is older than 6 hours. It SHALL keep the last valid copy on the device and use it whenever a fetch fails.

#### Scenario: Show added remotely
- **WHEN** a show is added to the remote file and the launcher is restarted
- **THEN** the new show tile appears without rebuilding or reinstalling the launcher

#### Scenario: Offline start
- **WHEN** the device has no network but a cached config exists
- **THEN** the show rows from the cached config are shown

#### Scenario: Invalid remote file
- **WHEN** the fetched file is not valid JSON or has an unsupported `version`
- **THEN** the cached config stays in use and the invalid file is not stored

### Requirement: One row per app with shows
For each config row whose app is installed, the home screen SHALL show a horizontally scrollable row. Its first tile is the app's banner card and its following tiles are the configured shows, in config order. Show rows SHALL appear below Favorites and above All apps. Rows for apps that aren't installed SHALL be hidden.

#### Scenario: Row layout
- **WHEN** the config has two Netflix shows and Netflix is installed
- **THEN** a row shows the Netflix app tile followed by the two show tiles

#### Scenario: App not installed
- **WHEN** a config row names an app that isn't installed
- **THEN** no row is shown for it

#### Scenario: D-pad through a row
- **WHEN** focus is on a row's app tile and the user presses right repeatedly
- **THEN** focus moves through the show tiles and the row scrolls to keep the focused tile visible

### Requirement: Open a show by deep link
Selecting a show tile SHALL start the show's `link` as an `ACTION_VIEW` intent restricted to the row's app. If no activity resolves the intent, the launcher SHALL open the app's launch activity instead. Selecting the app tile SHALL open the app.

#### Scenario: Deep link works
- **WHEN** the user selects a show tile whose link the app supports
- **THEN** the app opens on that show, possibly after the app's own profile picker

#### Scenario: Deep link unsupported
- **WHEN** the user selects a show tile whose link no activity in the app handles
- **THEN** the app opens on its own start screen

### Requirement: Artwork fetched once and cached
Each show's artwork SHALL be downloaded once from its `image` URL, scaled to the tile size and stored on the device. It SHALL only be downloaded again when the URL changes. Cached artwork no longer referenced by the config SHALL be deleted after a successful config fetch. A tile without artwork SHALL show the show's title.

#### Scenario: Return home
- **WHEN** the user returns to the home screen and the config hasn't changed
- **THEN** no artwork is downloaded and the show rows are not recomposed

#### Scenario: Artwork unavailable
- **WHEN** an image URL fails to download
- **THEN** the tile shows the show title and the download is retried at the next config refresh
