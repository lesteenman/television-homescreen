package me.steenman.tvlauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** A launchable TV app as shown on the home screen. */
data class LauncherApp(
    val component: ComponentName,
    val label: String,
    /** 16:9 banner (activity banner, falling back to the application banner), or null. */
    val banner: ImageBitmap?,
    /** Used on the card when there is no banner. */
    val icon: ImageBitmap?,
) {
    val key: String get() = component.flattenToString()
    val packageName: String get() = component.packageName
}

/** Lists every activity that resolves MAIN + LEANBACK_LAUNCHER, except this launcher. */
class AppRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    /**
     * Loaded entries by component. Reusing the same instances keeps an unchanged app list
     * equal to the previous one, so returning home neither reloads banners nor recomposes.
     */
    private val cache = ConcurrentHashMap<ComponentName, LauncherApp>()

    suspend fun loadApps(): List<LauncherApp> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val components = pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            .map { it.activityInfo }
            .filter { it.packageName != context.packageName }
            .associateBy { ComponentName(it.packageName, it.name) }

        cache.keys.retainAll(components.keys)
        components
            .map { (component, info) -> cache.getOrPut(component) { load(component, info) } }
            .sortedBy { it.label.lowercase() }
    }

    /** Drops cached entries for [packageName] so the next [loadApps] reloads them. */
    fun invalidate(packageName: String) {
        cache.keys.removeAll { it.packageName == packageName }
    }

    private fun load(component: ComponentName, info: ActivityInfo): LauncherApp {
        // ComponentInfo.loadBanner already falls back to the application banner.
        val banner = info.loadBanner(pm)
            ?.toBitmap(BANNER_WIDTH_PX, BANNER_HEIGHT_PX)
            ?.asImageBitmap()
        return LauncherApp(
            component = component,
            label = info.loadLabel(pm).toString(),
            banner = banner,
            icon = if (banner != null) {
                null
            } else {
                info.loadIcon(pm)?.toBitmap(ICON_SIZE_PX, ICON_SIZE_PX)?.asImageBitmap()
            },
        )
    }

    fun launchIntent(app: LauncherApp): Intent =
        Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

    private companion object {
        const val BANNER_WIDTH_PX = 320
        const val BANNER_HEIGHT_PX = 180
        const val ICON_SIZE_PX = 128
    }
}
