package com.ekquickfind.app.floating

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleGeometryTest {
    private val screen = BubbleGeometry(screenWidth = 1440, screenHeight = 3088, sizePx = 200, marginPx = 16)

    @Test
    fun startsOnTheRightEdgeAtItsSavedHeight() {
        assertEquals(1224 to 1155, screen.place(onRight = true, yFraction = 0.4f))
    }

    @Test
    fun theLeftSideKeepsTheSameMargin() {
        assertEquals(16, screen.place(onRight = false, yFraction = 0.4f).first)
    }

    @Test
    fun heightsOffTheScreenAreClampedToIt() {
        assertEquals(0, screen.place(onRight = true, yFraction = -1f).second)
        assertEquals(2888, screen.place(onRight = true, yFraction = 2f).second)
    }

    @Test
    fun lettingGoSnapsToTheNearerSideAndRemembersTheHeight() {
        val left = screen.snap(x = 300, y = 1444)
        assertFalse(left.onRight)
        assertEquals(0.5f, left.yFraction, 0.001f)
        assertTrue(screen.snap(x = 900, y = 0).onRight)
    }

    @Test
    fun aSmallWobbleIsATapNotADrag() {
        assertTrue(isTap(dx = 6f, dy = -5f, slopPx = 24))
        assertFalse(isTap(dx = 30f, dy = 0f, slopPx = 24))
    }
}
