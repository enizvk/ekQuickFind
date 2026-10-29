package com.ekquickfind.app.settings

import com.ekquickfind.app.ui.settings.dragTarget
import com.ekquickfind.app.ui.settings.moved
import org.junit.Assert.assertEquals
import org.junit.Test

class ReorderTest {
    @Test
    fun movingAnItemUpShiftsTheOthersDown() {
        assertEquals(listOf("a", "d", "b", "c"), listOf("a", "b", "c", "d").moved(from = 3, to = 1))
    }

    @Test
    fun movingAnItemDownShiftsTheOthersUp() {
        assertEquals(listOf("b", "c", "a", "d"), listOf("a", "b", "c", "d").moved(from = 0, to = 2))
    }

    @Test
    fun aRowStaysPutUntilDraggedPastHalfARow() {
        assertEquals(2, dragTarget(from = 2, offsetPx = -49f, rowHeightPx = 100, count = 6))
        assertEquals(1, dragTarget(from = 2, offsetPx = -51f, rowHeightPx = 100, count = 6))
    }

    @Test
    fun dragsPastTheEndsStopAtTheFirstAndLastRow() {
        assertEquals(0, dragTarget(from = 1, offsetPx = -900f, rowHeightPx = 100, count = 6))
        assertEquals(5, dragTarget(from = 4, offsetPx = 900f, rowHeightPx = 100, count = 6))
    }
}
