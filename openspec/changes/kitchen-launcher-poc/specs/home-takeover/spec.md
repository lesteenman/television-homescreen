## ADDED Requirements

### Requirement: Approaches tried in order E, B, C
The takeover SHALL be attempted on the kitchen device in this order: E (assign the HOME role), then B (disable the stock launcher, plus setupwraith if needed), then C (accessibility watchdog). Work SHALL stop at the first approach that meets the stickiness criteria. The result of each attempt SHALL be recorded in `design.md`.

#### Scenario: E succeeds
- **WHEN** approach E meets the stickiness criteria
- **THEN** approaches B and C are not applied to the device

#### Scenario: E fails
- **WHEN** after approach E, HOME still opens the stock launcher or the role reverts
- **THEN** the device is restored to stock and approach B is tried

### Requirement: Stickiness criteria
An approach SHALL count as working only if HOME opens the custom launcher on 10 out of 10 presses from inside other apps, after a device reboot, and after 24 hours of normal use.

#### Scenario: Reboot check
- **WHEN** the device reboots with the approach applied
- **THEN** the first home screen shown after boot is the custom launcher

### Requirement: Every device change has a tested undo
Each takeover step SHALL come with a script that applies it and a documented command that undoes it. The undo SHALL be tested before the next step is attempted. A `restore-stock` script SHALL undo all takeover changes at once.

#### Scenario: Restore to stock
- **WHEN** `scripts/restore-stock.sh` is run against the kitchen device
- **THEN** launcherx and setupwraith are enabled, the HOME role is held by launcherx, our accessibility service is disabled, and HOME opens the stock launcher

### Requirement: Disable setupwraith only when needed
Approach B SHALL disable `com.google.android.tungsten.setupwraith` only if, with launcherx disabled, its `RecoveryActivity` is observed handling HOME instead of our launcher.

#### Scenario: Our launcher wins without disabling setupwraith
- **WHEN** launcherx is disabled and HOME opens our launcher
- **THEN** setupwraith stays enabled

### Requirement: Accessibility watchdog (approach C)
If approach C is used, the launcher APK SHALL include an accessibility service that, when the stock launcher's home activity comes to the foreground, brings the custom launcher to the foreground. It SHALL do nothing else, and SHALL NOT read window content beyond package and class names.

#### Scenario: Stock launcher appears
- **WHEN** launcherx's HomeActivity becomes the foreground window
- **THEN** the custom launcher is started within 500 ms

### Requirement: Twilight removed
Twilight (`com.urbandroid.lux`) SHALL be uninstalled from the kitchen device before the takeover spike, so that its accessibility service is gone.

#### Scenario: Twilight gone
- **WHEN** `pm list packages com.urbandroid.lux` is run on the kitchen device
- **THEN** nothing is returned, and `enabled_accessibility_services` does not contain Twilight

### Requirement: Recovery access preserved
At every step, Settings SHALL stay reachable from the custom launcher and wireless adb SHALL stay reachable after a reboot.

#### Scenario: adb after reboot
- **WHEN** the device reboots with a takeover applied
- **THEN** the device is reachable via `adb connect` at the port advertised over mDNS
