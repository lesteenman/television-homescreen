package me.steenman.tvlauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock

/**
 * Starts the system screensaver (Ambient on Google TV) through the Settings app's "sleep" voice
 * action, which any app may send. Normal apps can't start a dream directly.
 */
object Screensaver {
    private const val ACTION_SLEEP = "com.google.android.pano.action.SLEEP"

    val intent: Intent get() = Intent(ACTION_SLEEP)

    fun isAvailable(packageManager: PackageManager): Boolean = intent.resolveActivity(packageManager) != null
}

/**
 * Decides whether a BACK press on the home screen may start the screensaver: only a fresh press
 * (not a held key), and not right after returning home, when it's usually the last of several
 * BACK presses that closed an app.
 */
class BackToScreensaverGuard(private val now: () -> Long = SystemClock::uptimeMillis) {
    private var resumedAt = Long.MIN_VALUE / 2

    fun onResume() {
        resumedAt = now()
    }

    fun allows(repeatCount: Int): Boolean = repeatCount == 0 && now() - resumedAt >= SETTLE_MS

    companion object {
        const val SETTLE_MS = 1_500L
    }
}
