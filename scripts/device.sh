#!/usr/bin/env bash
# Connects to the TV over wireless adb, finding its current port via mDNS
# (the port changes after every reboot). Prints the adb serial on stdout.
set -euo pipefail
source "$(dirname "$0")/common.sh"

services=$("$ADB" mdns services 2>/dev/null |
  awk -v name="$TV_MDNS_NAME" 'index($1, name) == 1 && $2 ~ /_adb-tls-connect/ { print $1, $3 }' | sort -u)
count=$(grep -c . <<<"$services" || true)
[[ "$count" -le 1 ]] || die "several devices advertise wireless debugging; set TV_MDNS_NAME in scripts/local.env:
$services"
addr=$(awk '{ print $2 }' <<<"$services")

[[ -n "$addr" ]] || die "no wireless-debugging service found${TV_MDNS_NAME:+ for '$TV_MDNS_NAME'}.
  On the TV: Settings > System > Developer options > Wireless debugging must be ON.
  If the TV was reset or unpaired, pair again: adb pair <ip>:<pairing-port> <code>"

"$ADB" connect "$addr" >&2
state=$("$ADB" -s "$addr" get-state 2>/dev/null || true)
[[ "$state" == "device" ]] || die "$addr is '${state:-unreachable}' (pair again if this persists)"

echo "$addr"
