package com.ekquickfind.app.ui.search

import com.ekquickfind.app.ui.search.SurfacePart.Field
import com.ekquickfind.app.ui.search.SurfacePart.Panel
import com.ekquickfind.app.ui.search.SurfacePart.Strip
import org.junit.Assert.assertEquals
import org.junit.Test

class SurfacePartsTest {
    @Test
    fun bottomBarWithTheStripOnTopPutsItAboveTheResults() {
        assertEquals(listOf(Strip, Panel, Field), surfaceParts(barAtTop = false, stripOnTop = true))
    }

    @Test
    fun bottomBarWithTheStripAtTheBottomKeepsItByTheBar() {
        assertEquals(listOf(Panel, Strip, Field), surfaceParts(barAtTop = false, stripOnTop = false))
    }

    @Test
    fun topBarWithTheStripOnTopKeepsItUnderTheBar() {
        assertEquals(listOf(Field, Strip, Panel), surfaceParts(barAtTop = true, stripOnTop = true))
    }

    @Test
    fun topBarWithTheStripAtTheBottomPutsItUnderTheResults() {
        assertEquals(listOf(Field, Panel, Strip), surfaceParts(barAtTop = true, stripOnTop = false))
    }
}
