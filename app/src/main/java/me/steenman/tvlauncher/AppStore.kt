package me.steenman.tvlauncher

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Opens the Play Store so new apps can be installed. Whether the store has a MAIN +
 * LEANBACK_LAUNCHER entry differs per device (the kitchen Chromecast HD has one), so this tries
 * the store's launcher entries first, then the generic "app market" intent, then a Play Store
 * search link.
 */
object AppStore {
    const val PLAY_STORE = "com.android.vending"

    /** The first way to open the store that resolves on this device, or null if none does. */
    fun intent(packageManager: PackageManager): Intent? =
        launcherActivity(packageManager, Intent.CATEGORY_LEANBACK_LAUNCHER)
            ?: launcherActivity(packageManager, Intent.CATEGORY_LAUNCHER)
            ?: Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MARKET)
                .takeIf { it.resolveActivity(packageManager) != null }
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q="))
                .setPackage(PLAY_STORE)
                .takeIf { it.resolveActivity(packageManager) != null }

    /**
     * The store's activity for MAIN + [category], as an explicit intent. Launcher activities
     * usually lack CATEGORY_DEFAULT, so an implicit intent with only the package set wouldn't start.
     */
    private fun launcherActivity(packageManager: PackageManager, category: String): Intent? {
        val query = Intent(Intent.ACTION_MAIN).addCategory(category).setPackage(PLAY_STORE)
        val info = packageManager
            .queryIntentActivities(query, PackageManager.ResolveInfoFlags.of(0))
            .firstOrNull()?.activityInfo
            ?: return null
        return Intent(Intent.ACTION_MAIN)
            .addCategory(category)
            .setComponent(ComponentName(info.packageName, info.name))
            .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    }
}
