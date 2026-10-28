#!/usr/bin/env bash
# Approach E: make our launcher the HOME role holder. Nothing is disabled.
# Undo: scripts/restore-stock.sh
set -euo pipefail
source "$(dirname "$0")/common.sh"
SERIAL=$("$(dirname "$0")/device.sh")

tv pm list packages "$APP_ID" | grep -q "$APP_ID" || die "$APP_ID is not installed; run scripts/deploy.sh first"

tv cmd role add-role-holder "$HOME_ROLE" "$APP_ID"
tv input keyevent KEYCODE_HOME
echo "Approach E applied:"
home_status
