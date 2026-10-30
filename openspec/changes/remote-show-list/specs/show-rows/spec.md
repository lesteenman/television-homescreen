## MODIFIED Requirements

### Requirement: Show configuration bundled with the app
The launcher SHALL read its show rows from a per-TV file in a remote location set at build time (`shows.url`), named after the TV's device name as in design.md (D1), using the same JSON format as the bundled `assets/shows.json`. It SHALL keep the last valid remote file on the device and show it on start. When there is no valid remote file yet, or no remote location is set, it SHALL show the bundled file.

#### Scenario: Show added
- **WHEN** a show is added to the TV's remote file and the home screen is opened more than 15 minutes after the last fetch (plus GitHub's cache delay)
- **THEN** the new show tile appears, without a rebuild or install

#### Scenario: Each TV its own list
- **WHEN** two TVs with different device names run the same APK
- **THEN** each shows the rows from its own file

#### Scenario: Offline start
- **WHEN** the device has no network
- **THEN** the last valid list is shown, with cached artwork where it was downloaded before

#### Scenario: Invalid or missing remote file
- **WHEN** the TV's remote file is missing, not valid JSON, or has an unsupported `version`
- **THEN** the list shown before stays, and the rest of the home screen works as before

#### Scenario: Invalid bundled file
- **WHEN** there is no valid remote file yet and `shows.json` in the APK is not valid
- **THEN** no show rows are shown and the rest of the home screen works as before

#### Scenario: No remote location
- **WHEN** the APK is built without `shows.url`
- **THEN** the launcher shows the bundled file and makes no request for a show list
