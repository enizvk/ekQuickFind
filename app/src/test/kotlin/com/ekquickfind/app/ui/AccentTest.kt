package com.ekquickfind.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.ekquickfind.app.ui.theme.darkFallback
import com.ekquickfind.app.ui.theme.hue
import com.ekquickfind.app.ui.theme.lightFallback
import com.ekquickfind.app.ui.theme.withAccent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AccentTest {
    private val red = Color(0xFFE53935)

    @Test
    fun systemAccentKeepsTheSchemeAsItIs() {
        assertSame(darkFallback, darkFallback.withAccent(null, dark = true))
    }

    @Test
    fun theAccentHueDrivesPrimaryAndTheSelectedItemBackground() {
        listOf(true to darkFallback, false to lightFallback).forEach { (dark, base) ->
            val scheme = base.withAccent(red, dark)
            assertEquals("primary dark=$dark", red.hue(), scheme.primary.hue(), 2f)
            assertEquals("selected dark=$dark", red.hue(), scheme.secondaryContainer.hue(), 2f)
        }
    }

    @Test
    fun darkModeGetsALighterAccentThanLightMode() {
        assertTrue(
            darkFallback.withAccent(red, dark = true).primary.luminance() > lightFallback.withAccent(red, dark = false).primary.luminance(),
        )
    }

    @Test
    fun textOnTheAccentStaysReadable() {
        listOf(true to darkFallback, false to lightFallback).forEach { (dark, base) ->
            val s = base.withAccent(red, dark)
            assertTrue("onPrimary dark=$dark", abs(s.primary.luminance() - s.onPrimary.luminance()) > 0.3f)
            assertTrue("onSecondaryContainer dark=$dark", abs(s.secondaryContainer.luminance() - s.onSecondaryContainer.luminance()) > 0.3f)
        }
    }
}
