package me.steenman.tvlauncher

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.steenman.tvlauncher.data.LauncherApp
import me.steenman.tvlauncher.data.shows.Show
import me.steenman.tvlauncher.ui.HomeScreen
import me.steenman.tvlauncher.ui.LauncherTheme

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    private val backGuard = BackToScreensaverGuard()

    /** Whether the BACK press now being handled may start the screensaver (see [dispatchKeyEvent]). */
    private var backStartsScreensaver = false

    /** Bumped on every HOME intent so the UI scrolls to the top and resets focus. */
    private val homeResets = mutableIntStateOf(0)

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val packageName = intent.data?.schemeSpecificPart ?: return
            if (intent.action == Intent.ACTION_PACKAGE_FULLY_REMOVED) {
                viewModel.onPackageFullyRemoved(packageName)
            } else {
                viewModel.onPackageChanged(packageName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screensaverAvailable = Screensaver.isAvailable(packageManager)

        // BACK on the home screen never leaves it; there is nothing "behind" a launcher.
        // It starts the screensaver instead, when the guard allows it.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (screensaverAvailable && backStartsScreensaver) startSafely(Screensaver.intent)
                backStartsScreensaver = false
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch { viewModel.keepShowsFresh() }
                launch { viewModel.keepAudienceCurrent() }
                viewModel.keepWeatherFresh()
            }
        }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val weather by viewModel.weather.collectAsStateWithLifecycle()
            LauncherTheme {
                HomeScreen(
                    state = state,
                    weather = weather,
                    resetSignal = homeResets.intValue,
                    onLaunch = ::launch,
                    onLaunchShow = ::launchShow,
                    onOpenSettings = { startSafely(Intent(Settings.ACTION_SETTINGS)) },
                    onAppInfo = { app ->
                        startSafely(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", app.packageName, null),
                            ),
                        )
                    },
                    onAddFavorite = viewModel::addFavorite,
                    onRemoveFavorite = viewModel::removeFavorite,
                    onMoveFavorite = viewModel::moveFavorite,
                    onStartScreensaver = if (screensaverAvailable) {
                        { startSafely(Screensaver.intent) }
                    } else {
                        null
                    },
                    onToggleAudience = viewModel::toggleAudience,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onResume() {
        super.onResume()
        backGuard.onResume()
        viewModel.refresh()
    }

    override fun onStop() {
        unregisterReceiver(packageReceiver)
        super.onStop()
    }

    // The back callback doesn't see the key event, so the guard decides on BACK's key-down.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
            backStartsScreensaver = backGuard.allows(event.repeatCount)
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homeResets.intValue++
    }

    private fun launch(app: LauncherApp) = startSafely(viewModel.repository.launchIntent(app))

    /** Opens [show] in [app] by its deep link, or just opens the app when the link isn't supported. */
    private fun launchShow(app: LauncherApp, show: Show) {
        val intent = showIntent(packageManager, app.packageName, show.link)
        if (intent != null) startSafely(intent) else launch(app)
    }

    private fun startSafely(intent: Intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Cannot start $intent", e)
        }
    }

    private companion object {
        const val TAG = "TvLauncher"
    }
}

/** The deep-link intent for [link] in [packageName], or null if that app has nothing to handle it. */
fun showIntent(packageManager: PackageManager, packageName: String, link: String): Intent? =
    Intent(Intent.ACTION_VIEW, Uri.parse(link))
        .setPackage(packageName)
        .apply { if (packageName == NETFLIX) netflixPartnerLaunch() }
        .takeIf { it.resolveActivity(packageManager) != null }

private const val NETFLIX = "com.netflix.ninja"

/**
 * Netflix ignores a link sent to its running task, and loses it behind the profile picker on a
 * cold start. Launched like a TV partner app (a fresh task plus `source=30`), it opens the title
 * in both cases (tested on the kitchen TV, 2026-10-28).
 */
private fun Intent.netflixPartnerLaunch() {
    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    putExtra("source", "30")
}
