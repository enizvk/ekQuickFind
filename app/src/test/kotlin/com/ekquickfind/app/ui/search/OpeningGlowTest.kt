package com.ekquickfind.app.ui.search

import com.ekquickfind.data.prefs.BarAnimation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningGlowTest {
    @Test
    fun theSweepFadesInHoldsAndEndsInvisible() {
        assertEquals(0f, envelope(0f, fadeIn = 0.1f, fadeOut = 0.25f), 0f)
        assertEquals(1f, envelope(0.5f, fadeIn = 0.1f, fadeOut = 0.25f), 0f)
        assertEquals(0f, envelope(1f, fadeIn = 0.1f, fadeOut = 0.25f), 0f)
    }

    @Test
    fun thePulseHitsEachKeyframeAndEndsInvisible() {
        val values = floatArrayOf(0f, 0.9f, 0.5f, 0.85f, 0.4f, 0f)
        assertEquals(0f, keyframes(0f, values), 0f)
        assertEquals(0.9f, keyframes(0.2f, values), 1e-4f)
        assertEquals(0.5f, keyframes(0.4f, values), 1e-4f)
        assertEquals(0f, keyframes(1f, values), 0f)
    }

    @Test
    fun thePulseMovesSmoothlyBetweenKeyframes() {
        val values = floatArrayOf(0f, 1f)
        val mid = keyframes(0.5f, values)
        assertEquals(0.5f, mid, 1e-4f)
        assertTrue(keyframes(0.1f, values) < 0.1f)
    }

    @Test
    fun everyBarAnimationStartsAndEndsInvisible() {
        BarAnimation.entries.forEach { animation ->
            listOf(0f, 1f).forEach { p ->
                val (line, halo) = glowAlphas(animation, p)
                assertEquals("$animation line at $p", 0f, line, 1e-4f)
                assertEquals("$animation halo at $p", 0f, halo, 1e-4f)
            }
        }
    }

    @Test
    fun everyBarAnimationIsClearlyVisibleHalfway() {
        BarAnimation.entries.forEach { animation ->
            assertTrue("$animation", glowAlphas(animation, 0.5f).first > 0.3f)
        }
    }

    @Test
    fun everyBarAnimationLastsAFewSeconds() {
        BarAnimation.entries.forEach { animation ->
            assertTrue("$animation", glowDurationMillis(animation) in 2000..4000)
        }
    }
}
