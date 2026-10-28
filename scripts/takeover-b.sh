#!/usr/bin/env bash
# Approach B: disable the stock launcher so a HOME press falls through to us.
#   scripts/takeover-b.sh                    -> disable launcherx only
#   scripts/takeover-b.sh --with-setupwraith -> also disable the setup wizard,
#                                               whose RecoveryActivity (priority 1) outranks us
# Our launcher also takes the HOME role. Undo: scripts/restore-stock.sh
set -euo pipefail
source "$(dirname "$0")/common.sh"
SERIAL=$("$(dirname "$0")/device.sh")

tv pm list packages "$APP_ID" | grep -q "$APP_ID" || die "$APP_ID is not installed; run scripts/deploy.sh first"

tv cmd role add-role-holder "$HOME_ROLE" "$APP_ID"
tv pm disable-user --user 0 "$STOCK_LAUNCHER"
if [[ "${1:-}" == "--with-setupwraith" ]]; then
  tv pm disable-user --user 0 "$SETUP_WRAITH"
fi

tv input keyevent KEYCODE_HOME
echo "Approach B applied:"
home_status
