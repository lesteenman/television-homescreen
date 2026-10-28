#!/usr/bin/env bash
# Builds and installs the debug launcher, then opens it.
#   scripts/deploy.sh            -> kitchen TV (wireless adb)
#   scripts/deploy.sh emulator   -> Google TV emulator (started if needed)
set -euo pipefail
source "$(dirname "$0")/common.sh"

case "${1:-tv}" in
  tv) serial=$("$(dirname "$0")/device.sh") ;;
  emulator) serial=$("$(dirname "$0")/emulator.sh") ;;
  *) die "usage: $0 [tv|emulator]" ;;
esac

cd "$REPO_ROOT"
ANDROID_SERIAL="$serial" ./gradlew -q installDebug
"$ADB" -s "$serial" shell am start -n "$APP_ID/.MainActivity" >/dev/null
echo "Installed and started on $serial"
