#!/usr/bin/env bash
# Builds and installs the launcher, then opens it.
#   scripts/deploy.sh                      -> debug build on the kitchen TV (wireless adb)
#   scripts/deploy.sh emulator             -> debug build on the Google TV emulator (started if needed)
#   scripts/deploy.sh tv --release         -> release build (needs keystore.properties)
# With more than one TV, pick it with TV=<name>, e.g. TV=living-room scripts/deploy.sh tv --release
# Debug and release are signed differently: switching on a device needs an uninstall first.
set -euo pipefail
source "$(dirname "$0")/common.sh"

case "${1:-tv}" in
  tv) serial=$("$(dirname "$0")/device.sh") ;;
  emulator) serial=$("$(dirname "$0")/emulator.sh") ;;
  *) die "usage: $0 [tv|emulator]" ;;
esac

cd "$REPO_ROOT"
variant="Debug"
[[ "${2:-}" == "--release" ]] && variant="Release"
ANDROID_SERIAL="$serial" ./gradlew -q "install$variant"
"$ADB" -s "$serial" shell am start -n "$APP_ID/.MainActivity" >/dev/null
echo "Installed and started on $serial"
