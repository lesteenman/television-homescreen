## MODIFIED Requirements

### Requirement: Back and Home behaviour
Pressing BACK on the home screen SHALL NOT leave the launcher; it SHALL start the system screensaver as specified by `screensaver-shortcut`, or do nothing when that isn't available. Pressing HOME while the launcher is already showing SHALL scroll to the top and reset focus as on initial open.

#### Scenario: Back on home
- **WHEN** the user presses BACK on the home screen
- **THEN** the launcher doesn't close: the screensaver starts, or nothing happens

#### Scenario: Home on home
- **WHEN** the user has scrolled down and presses HOME
- **THEN** the view scrolls to the top and focus returns to the initial element
