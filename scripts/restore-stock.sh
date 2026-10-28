#!/usr/bin/env bash
# Undoes every HOME-takeover change (approaches E, B and C) and returns the TV
# to the stock Google TV launcher. Safe to run on an untouched device.
set -euo pipefail
source "$(dirname "$0")/common.sh"
SERIAL=$("$(dirname "$0")/device.sh")

echo "Re-enabling stock packages (B)..."
tv pm default-state --user 0 "$STOCK_LAUNCHER" >/dev/null
tv pm default-state --user 0 "$SETUP_WRAITH" >/dev/null

echo "Giving the HOME role back to the stock launcher (E)..."
tv cmd role add-role-holder "$HOME_ROLE" "$STOCK_LAUNCHER"

echo "Removing our accessibility service (C)..."
services=$(tv settings get secure enabled_accessibility_services)
remaining=$(tr ':' '\n' <<<"$services" | grep -v "^$APP_ID/" | grep -vx null | paste -sd: - || true)
if [[ -z "$remaining" ]]; then
  tv settings delete secure enabled_accessibility_services >/dev/null
else
  tv settings put secure enabled_accessibility_services "$remaining"
fi

tv input keyevent KEYCODE_HOME
echo "Done. Current state:"
home_status
