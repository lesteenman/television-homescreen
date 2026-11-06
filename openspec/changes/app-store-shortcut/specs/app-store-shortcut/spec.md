## ADDED Requirements

### Requirement: App store button
The home screen header SHALL show a Play Store button when an app store can be opened on the device. Selecting it SHALL open the store.

#### Scenario: Open the store
- **WHEN** the user selects the Play Store button
- **THEN** the Play Store opens

#### Scenario: No store available
- **WHEN** none of the store intents resolves
- **THEN** the Play Store button is not shown

### Requirement: Store intent fallbacks
The launcher SHALL try, in order: the Play Store's LEANBACK_LAUNCHER activity, its LAUNCHER activity, the system's APP_MARKET selector, and a `market://search` link to the Play Store, using the first that resolves.

#### Scenario: No leanback entry
- **WHEN** the Play Store has no LEANBACK_LAUNCHER activity but the APP_MARKET selector resolves
- **THEN** the button opens the store through the APP_MARKET selector
