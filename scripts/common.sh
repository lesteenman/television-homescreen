# Shared settings for the helper scripts. Source it; don't run it.

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="${ADB:-$SDK/platform-tools/adb}"
APP_ID="me.steenman.tvlauncher"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Which TV to use when there are several: TV=<name>, looked up as TV_MDNS_<NAME> (upper case,
# dashes as underscores) in scripts/local.env, which is git-ignored because the mDNS names
# ("adb-<serial>") contain serial numbers. For example:
#   TV_MDNS_KITCHEN=adb-<serial>
#   TV_MDNS_LIVING_ROOM=adb-<serial>
# Without TV, scripts/device.sh uses the only device advertising wireless debugging.
[[ -f "$REPO_ROOT/scripts/local.env" ]] && source "$REPO_ROOT/scripts/local.env"
if [[ -n "${TV:-}" ]]; then
  tv_var="TV_MDNS_$(tr 'a-z-' 'A-Z_' <<<"$TV")"
  TV_MDNS_NAME="${!tv_var:-}"
  [[ -n "$TV_MDNS_NAME" ]] || { echo "error: TV=$TV but $tv_var is not set in scripts/local.env" >&2; exit 1; }
fi
TV_MDNS_NAME="${TV_MDNS_NAME:-}"

die() {
  echo "error: $*" >&2
  exit 1
}

STOCK_LAUNCHER="com.google.android.apps.tv.launcherx"
SETUP_WRAITH="com.google.android.tungsten.setupwraith"
HOME_ROLE="android.app.role.HOME"

# Runs a shell command on the TV and strips the carriage returns adb adds.
tv() {
  "$ADB" -s "$SERIAL" shell "$@" | tr -d '\r'
}

# Prints the HOME role holder and what a HOME intent resolves to right now.
home_status() {
  echo "  role holder : $(tv cmd role get-role-holders "$HOME_ROLE")"
  echo "  HOME intent : $(tv cmd package resolve-activity --brief \
    -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1)"
}
