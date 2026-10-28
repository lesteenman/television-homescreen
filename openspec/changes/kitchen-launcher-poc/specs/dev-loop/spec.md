## ADDED Requirements

### Requirement: Google TV emulator for UI work
The project SHALL document, and script, how to create a Google TV emulator (`system-images;android-34;google-tv;arm64-v8a`, 1080p TV profile) on the Mac and install the launcher into it.

#### Scenario: Preview on laptop
- **WHEN** the developer runs the emulator script and then installs the debug build
- **THEN** the launcher runs in the Google TV emulator and can be opened from the emulator's app list or via HOME

### Requirement: Deploy to the kitchen device over wireless adb
A script SHALL find the kitchen device's current adb connect port via `adb mdns services`, connect to it, and install the debug build.

#### Scenario: Port changed after reboot
- **WHEN** the device has rebooted and its connect port has changed
- **THEN** the deploy script still connects and installs without manual port entry

#### Scenario: Device not found
- **WHEN** no `_adb-tls-connect` service is advertised
- **THEN** the script exits non-zero with a message to enable wireless debugging on the TV

### Requirement: Reproducible build
The launcher SHALL build with `./gradlew assembleDebug` from a clean checkout, using the Gradle wrapper and no IDE-only steps.

#### Scenario: Clean build
- **WHEN** `./gradlew assembleDebug` runs in a fresh clone
- **THEN** it produces an installable debug APK
