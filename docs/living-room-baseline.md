# Living room Google TV Streamer baseline

Captured 2026-10-30 before any launcher changes, so `scripts/restore-stock.sh` has a known target.

## Build
```
model:       Google TV Streamer (kirkwood)
android:     14 (SDK 34)
fingerprint: google/kirkwood/kirkwood:14/UTTK.260317.003/15205007:user/release-keys
display:     1920x1080 at density 320 (same as the kitchen Chromecast HD)
launcherx:   versionName=1.0.1002449598
```

## HOME role holder
```
com.google.android.apps.tv.launcherx
```

## HOME handlers
```
3 activities found:
  Activity #0:
    priority=2 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true
    com.google.android.apps.tv.launcherx/.home.HomeActivity
  Activity #1:
    priority=1 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true
    com.google.android.tungsten.setupwraith/.RecoveryActivity
  Activity #2:
    priority=-1000 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true
    com.android.tv.settings/.system.FallbackHome
```

## Disabled packages
```
package:com.google.android.apps.education.cast2class
package:com.android.tv.settings
package:com.google.android.tvrecommendations
```
`com.android.tv.settings` appears in this list, but `dumpsys package` reports it as `enabled=0` (default) and Settings opens normally, including its sleep action (`DaydreamVoiceAction`).

## Accessibility
```
enabled_accessibility_services=null
accessibility_enabled=0
```

## Screensaver
```
screensaver_components=com.google.android.apps.tv.dreamx/.service.Backdrop
```

## Apps used by the launcher
Netflix, Disney+, Videoland and Spotify are installed, with the same packages as in the kitchen.
