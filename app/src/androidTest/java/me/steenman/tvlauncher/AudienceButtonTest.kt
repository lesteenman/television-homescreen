package me.steenman.tvlauncher

import android.content.ComponentName
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.steenman.tvlauncher.data.LauncherApp
import me.steenman.tvlauncher.data.shows.Audience
import me.steenman.tvlauncher.data.shows.Show
import me.steenman.tvlauncher.data.shows.ShowRowConfig
import me.steenman.tvlauncher.ui.HomeScreen
import me.steenman.tvlauncher.ui.LauncherTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AudienceButtonTest {

    @get:Rule
    val compose = createComposeRule()

    private val youtube = LauncherApp(
        ComponentName("com.google.android.youtube.tv", "com.google.android.youtube.tv.Main"),
        "YouTube",
        banner = null,
        icon = null,
    )
    private val config = listOf(
        ShowRowConfig(
            youtube.packageName,
            listOf(
                Show("Kids show", "https://www.youtube.com/watch?v=1", null, Audience.KIDS),
                Show("Grown-up show", "https://www.youtube.com/watch?v=2", null, Audience.GROWN_UPS),
                Show("Family show", "https://www.youtube.com/watch?v=3", null),
            ),
        ),
    )

    private fun show(initial: Audience?) {
        var audience by mutableStateOf(initial)
        compose.setContent {
            LauncherTheme {
                HomeScreen(
                    state = homeUiState(listOf(youtube), emptyList(), config, emptyMap(), audience),
                    weather = null,
                    resetSignal = 0,
                    onLaunch = {},
                    onLaunchShow = { _, _ -> },
                    onOpenSettings = {},
                    onAppInfo = {},
                    onAddFavorite = {},
                    onRemoveFavorite = {},
                    onMoveFavorite = { _, _ -> },
                    onToggleAudience = { audience = audience?.other },
                )
            }
        }
    }

    @Test
    fun kidsModeShowsKidsAndFamilyShows() {
        show(Audience.KIDS)
        compose.onNodeWithText("Kids").assertExists()
        compose.onNodeWithText("Kids show", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Family show", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Grown-up show", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun buttonSwitchesMode() {
        show(Audience.KIDS)
        // tv-material buttons are pressed with the select key, not by touch.
        compose.onNodeWithText("Kids").requestFocus()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithText("Grown-ups").assertExists()
        compose.onNodeWithText("Grown-up show", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Kids show", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun noScheduleNoButtonAndEveryShow() {
        show(null)
        compose.onNodeWithText("Kids").assertDoesNotExist()
        compose.onNodeWithText("Grown-ups").assertDoesNotExist()
        listOf("Kids show", "Grown-up show", "Family show").forEach {
            compose.onNodeWithText(it, useUnmergedTree = true).assertExists()
        }
    }
}
