#!/usr/bin/env bash
# Creates (once) and boots the Google TV emulator used for UI work.
# Prints the emulator's adb serial on stdout once it has booted.
set -euo pipefail
source "$(dirname "$0")/common.sh"

IMAGE="system-images;android-34;google-tv;arm64-v8a"
AVD="gtv-34"
EMULATOR="$SDK/emulator/emulator"
SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager"
AVDMANAGER="$SDK/cmdline-tools/latest/bin/avdmanager"

if ! "$EMULATOR" -list-avds | grep -qx "$AVD"; then
  if [[ ! -d "$SDK/system-images/android-34/google-tv/arm64-v8a" ]]; then
    echo "Downloading $IMAGE (1-2 GB)..." >&2
    "$SDKMANAGER" "$IMAGE" >&2
  fi
  echo "no" | "$AVDMANAGER" create avd --name "$AVD" --package "$IMAGE" --device tv_1080p >&2
fi

serial=$("$ADB" devices | awk '/^emulator-/ { print $1; exit }')
if [[ -z "$serial" ]]; then
  log="${TMPDIR:-/tmp}/$AVD.log"
  echo "Starting $AVD (log: $log)..." >&2
  nohup "$EMULATOR" -avd "$AVD" -no-snapshot-save >"$log" 2>&1 &
  for _ in $(seq 1 60); do
    serial=$("$ADB" devices | awk '/^emulator-/ { print $1; exit }')
    [[ -n "$serial" ]] && break
    sleep 2
  done
  [[ -n "$serial" ]] || die "emulator did not appear in adb devices; see $log"
fi

echo "Waiting for $serial to finish booting..." >&2
"$ADB" -s "$serial" wait-for-device
until [[ "$("$ADB" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
  sleep 2
done

echo "$serial"
