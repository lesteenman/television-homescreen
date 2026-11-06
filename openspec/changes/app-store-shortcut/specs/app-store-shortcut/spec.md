## ADDED Requirements

### Requirement: App store tile
The home screen SHALL show a Play Store tile after the last tile of All apps when an app store can be opened on the device. Selecting it SHALL open the store.

#### Scenario: Open the store
- **WHEN** the user selects the Play Store tile
- **THEN** the Play Store opens

#### Scenario: No store available
- **WHEN** none of the store intents resolves
- **THEN** the Play Store tile is not shown

### Requirement: Store intent fallbacks
The launcher SHALL try, in order: the Play Store's LEANBACK_LAUNCHER activity, its LAUNCHER activity, the system's APP_MARKET selector, and a `market://search` link to the Play Store, using the first that resolves.

#### Scenario: No leanback entry
- **WHEN** the Play Store has no LEANBACK_LAUNCHER activity but the APP_MARKET selector resolves
- **THEN** the tile opens the store through the APP_MARKET selector
