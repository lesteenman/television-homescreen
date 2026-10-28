#!/usr/bin/env bash
# Approach C: keep the stock launcher enabled (Ambient's Google Photos and account mode depend
# on it) and let our accessibility service bring our launcher up whenever the stock home screen
# appears. Re-enables anything approach B disabled. Undo: scripts/restore-stock.sh
set -euo pipefail
source "$(dirname "$0")/common.sh"
SERIAL=$("$(dirname "$0")/device.sh")
REDIRECT_SERVICE="$APP_ID/.HomeRedirectService"

tv pm list packages "$APP_ID" | grep -q "$APP_ID" || die "$APP_ID is not installed; run scripts/deploy.sh first"

# The service first, so it's already watching when the stock home screen comes back.
services=$(tv settings get secure enabled_accessibility_services)
others=$(tr ':' '\n' <<<"$services" | grep -v "^$APP_ID/" | grep -vx null | paste -sd: - || true)
tv settings put secure enabled_accessibility_services "${others:+$others:}$REDIRECT_SERVICE"
tv settings put secure accessibility_enabled 1

tv cmd role add-role-holder "$HOME_ROLE" "$APP_ID"
tv pm default-state --user 0 "$STOCK_LAUNCHER" >/dev/null
tv pm default-state --user 0 "$SETUP_WRAITH" >/dev/null

tv input keyevent KEYCODE_HOME
sleep 3
echo "Approach C applied:"
home_status
echo "  on screen   : $(tv dumpsys activity activities | grep -m1 topResumedActivity | grep -oE '[a-z.]+/[A-Za-z.]+')"
