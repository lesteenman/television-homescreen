package me.steenman.tvlauncher

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.steenman.tvlauncher.ui.HomeScreen
import me.steenman.tvlauncher.ui.LauncherTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreensaverButtonTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(onStartScreensaver: (() -> Unit)?) = compose.setContent {
        LauncherTheme {
            HomeScreen(
                state = HomeUiState(loaded = true),
                weather = null,
                resetSignal = 0,
                onLaunch = {},
                onLaunchShow = { _, _ -> },
                onOpenSettings = {},
                onAppInfo = {},
                onAddFavorite = {},
                onRemoveFavorite = {},
                onMoveFavorite = { _, _ -> },
                onStartScreensaver = onStartScreensaver,
            )
        }
    }

    @Test
    fun buttonShownWhenAvailable() {
        show {}
        compose.onNodeWithText("Screensaver").assertExists()
    }

    @Test
    fun buttonHiddenWhenUnavailable() {
        show(null)
        compose.onNodeWithText("Screensaver").assertDoesNotExist()
    }

    @Test
    fun sleepActionResolvesOnGoogleTv() {
        val pm = InstrumentationRegistry.getInstrumentation().targetContext.packageManager
        assertTrue(Screensaver.isAvailable(pm))
    }
}
