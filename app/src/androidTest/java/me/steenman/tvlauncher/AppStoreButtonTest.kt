package me.steenman.tvlauncher

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.steenman.tvlauncher.ui.HomeScreen
import me.steenman.tvlauncher.ui.LauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppStoreButtonTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(onOpenAppStore: (() -> Unit)?) = compose.setContent {
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
                onOpenAppStore = onOpenAppStore,
            )
        }
    }

    @Test
    fun buttonShownAndClickableWhenAvailable() {
        var opened = 0
        show { opened++ }
        compose.onNodeWithText("Play Store").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun buttonHiddenWhenUnavailable() {
        show(null)
        compose.onNodeWithText("Play Store").assertDoesNotExist()
    }

    /** The google-tv emulator image ships the Play Store, so one of the fallbacks must resolve. */
    @Test
    fun storeResolvesOnGoogleTv() {
        val pm = InstrumentationRegistry.getInstrumentation().targetContext.packageManager
        assertNotNull(AppStore.intent(pm))
    }
}
