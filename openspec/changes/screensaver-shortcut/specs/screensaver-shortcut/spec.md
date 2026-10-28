## ADDED Requirements

### Requirement: Screensaver button
The home screen header SHALL show a Screensaver button next to the Settings button when the system's sleep action (`com.google.android.pano.action.SLEEP`) is available. Selecting it SHALL start the system screensaver.

#### Scenario: Start from the header
- **WHEN** the user selects the Screensaver button
- **THEN** the configured system screensaver (Ambient) starts

#### Scenario: Sleep action unavailable
- **WHEN** no activity handles the sleep action
- **THEN** the Screensaver button is not shown

### Requirement: Screensaver on BACK
A fresh BACK press on the home screen SHALL start the system screensaver, unless the home screen was resumed less than 1.5 seconds earlier.

#### Scenario: Back on home
- **WHEN** the home screen has been showing for a while and the user presses BACK
- **THEN** the system screensaver starts

#### Scenario: Back while leaving an app
- **WHEN** the user presses BACK repeatedly to leave an app and one press arrives within 1.5 seconds of the home screen appearing
- **THEN** that press is ignored and the home screen stays
