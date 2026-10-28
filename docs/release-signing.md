# Release signing

Release builds are signed with a local key that never enters the repo.

- Keystore: `~/.android/me.steenman.tvlauncher-release.jks` (PKCS12, alias `tvlauncher`)
- Config: `keystore.properties` in the repo root (git-ignored) with `storeFile`, `storePassword`, `keyAlias`, `keyPassword`

Without `keystore.properties` the release build is produced unsigned and can't be installed.

**Back up the keystore.** Android only installs an update when it is signed with the same key. If the key is lost, the app must be uninstalled first, which wipes favorites.

Debug and release builds use different keys, so switching between them on the TV also means uninstalling first.
