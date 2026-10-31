#!/usr/bin/env bash
# Connects to the TV over wireless adb, finding its current port via mDNS
# (the port changes after every reboot). Prints the adb serial on stdout.
set -euo pipefail
source "$(dirname "$0")/common.sh"

services=$("$ADB" mdns services 2>/dev/null |
  awk -F'\t' -v name="$TV_MDNS_NAME" 'index($1, name) == 1 && $2 ~ /_adb-tls-connect/ { print $1, $3 }' | sort -u)
count=$(grep -c . <<<"$services" || true)
# One TV can be listed more than once: adb keeps its old port after wireless debugging restarts.
[[ "$count" -le 1 || -n "$TV_MDNS_NAME" ]] || die "several devices advertise wireless debugging; choose one with TV=<name> (see scripts/common.sh):
$services"

[[ -n "$services" ]] || die "no wireless-debugging service found${TV_MDNS_NAME:+ for '$TV_MDNS_NAME'}.
  On the TV: Settings > System > Developer options > Wireless debugging must be ON.
  If the TV was reset or unpaired, pair again: adb pair <ip>:<pairing-port> <code>"

# Use the first address that answers.
for addr in $(awk '{ print $NF }' <<<"$services"); do
  "$ADB" connect "$addr" >&2 || true
  if [[ "$("$ADB" -s "$addr" get-state 2>/dev/null || true)" == "device" ]]; then
    echo "$addr"
    exit 0
  fi
done
die "$(awk '{ print $NF }' <<<"$services" | paste -sd, -) unreachable (pair again if this persists)"
