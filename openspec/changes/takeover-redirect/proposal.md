## Why

Approach B (disabling launcherx and setupwraith) made our launcher the home screen, but it broke other system apps that depend on launcherx. Ambient lost Google Photos as a source, and the kitchen TV disappeared from Google Home. Ambient's log showed why: it asks launcherx's `AccountModeContentProvider` for the account mode and, without an answer, logs "account mode is not valid" and hides account features.

## What Changes

- Switch the kitchen TV from approach B to approach C: launcherx and setupwraith are enabled again, and an accessibility service in our APK brings our launcher up whenever launcherx's home screen comes to the front.
- New script `scripts/takeover-c.sh`; `scripts/restore-stock.sh` already undoes it.
- A narrower B (disabling only the HOME activities) was tried first and is impossible: Android refuses component changes from adb for non-test apps.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `home-takeover`: The approach order gains a rule that C replaces B when B breaks other system apps.

## Impact

- **Depends on** `kitchen-launcher-poc` being archived first, so that `home-takeover` exists as a main spec.
- Code: `HomeRedirectService` and its accessibility config in the APK.
- The stock home screen shows for about 0.15 to 0.3 s on each HOME press before ours replaces it (judged fine in use).
