## ADDED Requirements

### Requirement: Show configuration bundled with the app
The launcher SHALL read its show rows from a JSON file bundled in the APK (`assets/shows.json`), using the format in design.md (D2). The parser SHALL not depend on where the file comes from, so a remote source can be added later without changing the format.

#### Scenario: Show added
- **WHEN** a show is added to `shows.json` and the launcher is rebuilt and installed
- **THEN** the new show tile appears

#### Scenario: Offline start
- **WHEN** the device has no network
- **THEN** all show rows are shown, with cached artwork where it was downloaded before

#### Scenario: Invalid file
- **WHEN** `shows.json` is not valid JSON or has an unsupported `version`
- **THEN** no show rows are shown and the rest of the home screen works as before

### Requirement: One row per app with shows
For each config row whose app is installed, the home screen SHALL show a horizontally scrollable row. Its first tile is the app's banner card and its following tiles are the configured shows, in config order. Show rows SHALL appear directly below the header, above Favorites and All apps, so the first row's app tile has initial focus. Rows for apps that aren't installed SHALL be hidden.

#### Scenario: Row layout
- **WHEN** the config has two Netflix shows and Netflix is installed
- **THEN** a row shows the Netflix app tile followed by the two show tiles

#### Scenario: App not installed
- **WHEN** a config row names an app that isn't installed
- **THEN** no row is shown for it

#### Scenario: Show row replaces the favorite tile
- **WHEN** an app is a favorite and also has a show row
- **THEN** it does not appear in the Favorites row, and its favorite position is kept for when the show row goes away

#### Scenario: D-pad through a row
- **WHEN** focus is on a row's app tile and the user presses right repeatedly
- **THEN** focus moves through the show tiles and the row scrolls to keep the focused tile visible

### Requirement: Open a show by deep link
Selecting a show tile SHALL start the show's `link` as an `ACTION_VIEW` intent restricted to the row's app. If no activity resolves the intent, the launcher SHALL open the app's launch activity instead. Selecting the app tile SHALL open the app.

#### Scenario: Deep link works
- **WHEN** the user selects a show tile whose link the app supports
- **THEN** the app opens on that show (or, for music, starts playing it), possibly after the app's own profile picker

#### Scenario: Deep link unsupported
- **WHEN** the user selects a show tile whose link no activity in the app handles
- **THEN** the app opens on its own start screen

### Requirement: Artwork fetched once and cached
Each show's artwork SHALL be downloaded once from its `image` URL, scaled to the tile size and stored on the device. It SHALL only be downloaded again when the URL changes. Cached artwork no longer referenced by the config SHALL be deleted after the config is loaded. A tile without artwork SHALL show the show's title.

#### Scenario: Return home
- **WHEN** the user returns to the home screen and the config hasn't changed
- **THEN** no artwork is downloaded and the show rows are not recomposed

#### Scenario: Artwork unavailable
- **WHEN** an image URL fails to download
- **THEN** the tile shows the show title and the download is retried the next time the home screen resumes
