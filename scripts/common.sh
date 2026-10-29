# Shared settings for the helper scripts. Source it; don't run it.

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="${ADB:-$SDK/platform-tools/adb}"
APP_ID="me.steenman.tvlauncher"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Machine-specific settings stay out of git: scripts/local.env (git-ignored) may set
# TV_MDNS_NAME, the mDNS instance-name prefix of the TV ("adb-<serial>"). Without it,
# scripts/device.sh uses the only device advertising wireless debugging.
[[ -f "$REPO_ROOT/scripts/local.env" ]] && source "$REPO_ROOT/scripts/local.env"
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
