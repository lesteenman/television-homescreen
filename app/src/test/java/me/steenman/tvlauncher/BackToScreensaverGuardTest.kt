package me.steenman.tvlauncher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackToScreensaverGuardTest {

    private var clock = 10_000L
    private val guard = BackToScreensaverGuard { clock }

    @Test
    fun `fresh press after the home screen has settled is allowed`() {
        guard.onResume()
        clock += BackToScreensaverGuard.SETTLE_MS
        assertTrue(guard.allows(repeatCount = 0))
    }

    @Test
    fun `press right after returning home is ignored`() {
        guard.onResume()
        clock += 400
        assertFalse(guard.allows(repeatCount = 0))
    }

    @Test
    fun `held key is ignored`() {
        guard.onResume()
        clock += 60_000
        assertFalse(guard.allows(repeatCount = 3))
    }

    @Test
    fun `allowed before any resume`() {
        assertTrue(guard.allows(repeatCount = 0))
    }
}
