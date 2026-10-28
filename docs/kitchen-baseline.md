# Kitchen Chromecast baseline

Captured 2026-10-28 09:15 before any launcher changes, so `scripts/restore-stock.sh` has a known target.

## Build
```
model:       Chromecast HD (boreal)
android:     14 (SDK 34)
fingerprint: google/boreal/boreal:14/UTTC.250917.004/14193937:user/release-keys
patch:       2025-10-01
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
package:com.google.android.tvrecommendations
```

## Accessibility
```
enabled_accessibility_services=com.urbandroid.lux/com.urbandroid.lux.TwilightService
accessibility_enabled=1
```

## Screensaver
```
screensaver_components=com.google.android.apps.tv.dreamx/.service.Backdrop
```
