package me.steenman.tvlauncher

import android.content.ComponentName
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.steenman.tvlauncher.data.LauncherApp
import me.steenman.tvlauncher.data.shows.Show
import me.steenman.tvlauncher.data.shows.ShowRowConfig
import me.steenman.tvlauncher.ui.HomeScreen
import me.steenman.tvlauncher.ui.LauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShowRowsTest {

    @get:Rule
    val compose = createComposeRule()

    private val youtube = app("com.google.android.youtube.tv", "YouTube")
    private val store = app("com.android.vending", "Store")
    private val settings = app("com.android.tv.settings", "Settings app")

    private val shows = (1..6).map { Show("Show $it", "https://www.youtube.com/watch?v=$it", null) }
    private val config = listOf(
        ShowRowConfig(youtube.packageName, shows),
        ShowRowConfig("not.installed", shows),
    )

    private fun app(packageName: String, label: String) =
        LauncherApp(ComponentName(packageName, "$packageName.Main"), label, banner = null, icon = null)

    @Test
    fun rowsOnlyForInstalledAppsAndTheyReplaceFavoriteTiles() {
        val state = homeUiState(
            apps = listOf(store, settings, youtube),
            favoriteKeys = listOf(youtube.key, store.key),
            config = config,
            artwork = emptyMap(),
        )

        assertEquals(listOf(youtube), state.showRows.map { it.app })
        assertEquals(shows, state.showRows.single().shows.map { it.show })
        // YouTube has a row, so only Store is left in Favorites; YouTube is still a stored favorite.
        assertEquals(listOf(store), state.favorites)
        assertEquals(true, state.isFavorite(youtube))
    }

    @Test
    fun rowWithoutShowsKeepsTheFavorite() {
        val state = homeUiState(
            apps = listOf(youtube),
            favoriteKeys = listOf(youtube.key),
            config = listOf(ShowRowConfig(youtube.packageName, emptyList())),
            artwork = emptyMap(),
        )
        assertEquals(emptyList<ShowRow>(), state.showRows)
        assertEquals(listOf(youtube), state.favorites)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun appTileFirstThenShowsAlongTheRow() {
        val launched = mutableListOf<Pair<LauncherApp, Show>>()
        val state = homeUiState(listOf(store, youtube), emptyList(), config, emptyMap())
        compose.setContent {
            LauncherTheme {
                HomeScreen(
                    state = state,
                    weather = null,
                    resetSignal = 0,
                    onLaunch = {},
                    onLaunchShow = { app, show -> launched += app to show },
                    onOpenSettings = {},
                    onAppInfo = {},
                    onAddFavorite = {},
                    onRemoveFavorite = {},
                    onMoveFavorite = { _, _ -> },
                )
            }
        }

        // Initial focus is the row's app tile; each RIGHT moves one show along, scrolling as needed.
        compose.onNode(isFocused() and hasAnyDescendant(hasText("YouTube")), useUnmergedTree = true).assertExists()
        repeat(6) {
            compose.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
            compose.onNode(isFocused() and hasAnyDescendant(hasText("Show ${it + 1}")), useUnmergedTree = true)
                .assertExists()
        }

        // Show 6 has focus; tv-material cards are clicked with the select key, not by touch.
        compose.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
        assertEquals(listOf(youtube to shows[5]), launched)
    }

    @Test
    fun deepLinkResolvesOnlyWhenTheAppHandlesIt() {
        val pm = InstrumentationRegistry.getInstrumentation().targetContext.packageManager
        assertNotNull(showIntent(pm, youtube.packageName, "https://www.youtube.com/watch?v=aqz-KE-bpKQ"))
        // Falls back to opening the app.
        assertNull(showIntent(pm, youtube.packageName, "https://example.invalid/nothing-handles-this"))
    }
}
