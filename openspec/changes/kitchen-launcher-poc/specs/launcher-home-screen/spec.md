## ADDED Requirements

### Requirement: Launcher registers as a TV home activity
The launcher app SHALL declare a single activity with an intent filter containing `ACTION_MAIN`, `CATEGORY_HOME`, `CATEGORY_DEFAULT` and `CATEGORY_LEANBACK_LAUNCHER`.

#### Scenario: Appears as a HOME candidate
- **WHEN** `cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME` is run on a device with the launcher installed
- **THEN** the launcher's activity is listed

### Requirement: Shows all launchable TV apps
The home screen SHALL show a grid of every activity that resolves `ACTION_MAIN` + `CATEGORY_LEANBACK_LAUNCHER`, excluding the launcher itself. Each entry SHALL show the activity banner, falling back to the application banner and then to the app icon with its label.

#### Scenario: Installed app is listed
- **WHEN** an app with a leanback launcher activity is installed
- **THEN** it appears in the "All apps" grid without restarting the launcher

#### Scenario: Uninstalled app disappears
- **WHEN** a listed app is uninstalled
- **THEN** it is removed from the grid and from favorites

#### Scenario: App without banner
- **WHEN** an app has no banner
- **THEN** its card shows the app icon and label

### Requirement: Favorites rows
The home screen SHALL show the user's favorite apps in their saved order, in one or more rows above the "All apps" grid. The favorites area SHALL be hidden when there are no favorites. Favorites SHALL persist across launcher restarts and device reboots.

#### Scenario: Add a favorite
- **WHEN** the user long-presses an app card and chooses "Add to favorites"
- **THEN** the app is appended to the favorites row

#### Scenario: Reorder a favorite
- **WHEN** the user chooses "Move left" or "Move right" on a favorite
- **THEN** the favorite swaps places with its neighbour and the new order persists

#### Scenario: Remove a favorite
- **WHEN** the user chooses "Remove from favorites"
- **THEN** the app is removed from the favorites row but stays in the grid

#### Scenario: Persistence
- **WHEN** the device reboots
- **THEN** the favorites appear in the same order

### Requirement: Launch apps
Selecting an app card with the D-pad center/OK button SHALL launch that app's leanback launcher activity.

#### Scenario: Launch from grid
- **WHEN** the user focuses an app card and presses OK
- **THEN** the app opens

### Requirement: Clock
The home screen SHALL show the current time, following the device's 12/24-hour setting, and update it at least once a minute.

#### Scenario: Minute rollover
- **WHEN** the system minute changes while the home screen is visible
- **THEN** the displayed time updates within 1 second

### Requirement: Gradient background with no ads or recommendations
The home screen SHALL use a static gradient background drawn locally. It SHALL NOT show content recommendations, promoted apps, ads, or anything fetched from the network.

#### Scenario: Offline home screen
- **WHEN** the device has no network connection
- **THEN** the home screen renders fully, with the gradient, clock, favorites and grid

### Requirement: D-pad navigation and safe area
All interactive elements SHALL be reachable with the D-pad and have a visible focus state. Content SHALL stay within overscan-safe margins of at least 48 dp horizontally and 27 dp vertically.

#### Scenario: Initial focus
- **WHEN** the home screen opens
- **THEN** focus is on the first favorite, or on the first grid item if there are no favorites

### Requirement: Settings shortcut
The home screen SHALL offer a Settings tile that opens the system Settings app.

#### Scenario: Open settings
- **WHEN** the user selects the Settings tile
- **THEN** the system Settings app opens

### Requirement: Back and Home behaviour
Pressing BACK on the home screen SHALL NOT leave the launcher. Pressing HOME while the launcher is already showing SHALL scroll to the top and reset focus as on initial open.

#### Scenario: Back on home
- **WHEN** the user presses BACK on the home screen
- **THEN** the launcher stays visible

#### Scenario: Home on home
- **WHEN** the user has scrolled down and presses HOME
- **THEN** the view scrolls to the top and focus returns to the initial element
