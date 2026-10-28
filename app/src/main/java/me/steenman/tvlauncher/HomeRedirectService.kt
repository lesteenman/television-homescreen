package me.steenman.tvlauncher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Takeover approach C: the stock launcher stays enabled (other system apps depend on it, e.g.
 * Ambient's Google Photos), and whenever its home screen comes to the front (HOME, a boot, or an
 * app closing back to it) this service brings our launcher up instead. It only looks at the
 * package and class of new windows from the stock launcher.
 */
class HomeRedirectService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (event.packageName == STOCK_LAUNCHER && event.className?.startsWith(STOCK_HOME_PREFIX) == true) {
            goHome()
        }
    }

    override fun onInterrupt() = Unit

    private fun goHome() {
        // As a HOME intent, so the launcher also resets to the top as on a normal HOME press.
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setClass(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private companion object {
        const val STOCK_LAUNCHER = "com.google.android.apps.tv.launcherx"

        /** HomeActivity and VanillaModeHomeActivity; launcherx's other screens (profiles, account checks) stay. */
        const val STOCK_HOME_PREFIX = "$STOCK_LAUNCHER.home."
    }
}
