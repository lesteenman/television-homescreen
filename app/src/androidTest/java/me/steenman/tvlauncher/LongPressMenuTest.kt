package me.steenman.tvlauncher

import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.ViewConfiguration
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Holding OK on a remote sends a key-down, auto-repeated key-downs while held, then a
 * key-up on release. The menu opens during the hold; releasing must not pick an item.
 */
@RunWith(AndroidJUnit4::class)
class LongPressMenuTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var downTime = 0L

    @Before
    fun clearFavorites() {
        File(instrumentation.targetContext.filesDir, "datastore").deleteRecursively()
    }

    @Test
    fun releasingTheLongPressKeepsTheMenuOpen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(TIMEOUT_MS) { hasText("All apps") }
            compose.waitForIdle()

            holdCenter()
            releaseCenter()
            compose.waitForIdle()

            compose.onNodeWithText(ADD).assertExists()
            compose.onAllNodesWithText("Favorites").assertCountEquals(0)

            // Arrows move through the menu; a separate short press picks the focused item.
            press(KeyEvent.KEYCODE_DPAD_DOWN)
            press(KeyEvent.KEYCODE_DPAD_UP)
            compose.waitForIdle()
            press(KeyEvent.KEYCODE_DPAD_CENTER)
            compose.waitUntil(TIMEOUT_MS) { hasText("Favorites") }
            compose.onNodeWithText(ADD).assertDoesNotExist()
        }
    }

    private fun hasText(text: String) =
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    /**
     * Key-down, then remote-style auto-repeats that keep coming while the button is held:
     * until the menu is on screen, and a few more after that.
     */
    private fun holdCenter() {
        downTime = SystemClock.uptimeMillis()
        send(KeyEvent.ACTION_DOWN, repeat = 0)
        SystemClock.sleep(ViewConfiguration.getKeyRepeatTimeout().toLong())

        var repeatCount = 0
        fun autoRepeat() {
            repeatCount++
            val flags = if (repeatCount == 1) KeyEvent.FLAG_LONG_PRESS else 0
            send(KeyEvent.ACTION_DOWN, repeatCount, flags)
            SystemClock.sleep(REPEAT_INTERVAL_MS)
        }

        val deadline = SystemClock.uptimeMillis() + TIMEOUT_MS
        while (!hasText(ADD) && SystemClock.uptimeMillis() < deadline) autoRepeat()
        repeat(REPEATS_AFTER_MENU) { autoRepeat() }
    }

    private fun releaseCenter() = send(KeyEvent.ACTION_UP, repeat = 0)

    private fun press(keyCode: Int) {
        downTime = SystemClock.uptimeMillis()
        send(KeyEvent.ACTION_DOWN, repeat = 0, keyCode = keyCode)
        send(KeyEvent.ACTION_UP, repeat = 0, keyCode = keyCode)
    }

    private fun send(action: Int, repeat: Int, flags: Int = 0, keyCode: Int = KeyEvent.KEYCODE_DPAD_CENTER) {
        instrumentation.sendKeySync(
            KeyEvent(
                downTime, SystemClock.uptimeMillis(), action, keyCode, repeat,
                0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags or KeyEvent.FLAG_FROM_SYSTEM,
                InputDevice.SOURCE_DPAD,
            ),
        )
    }

    private companion object {
        const val ADD = "Add to favorites"
        const val TIMEOUT_MS = 5_000L
        const val REPEATS_AFTER_MENU = 10
        const val REPEAT_INTERVAL_MS = 50L
    }
}
