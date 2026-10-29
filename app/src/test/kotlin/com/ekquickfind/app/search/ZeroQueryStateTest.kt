package com.ekquickfind.app.search

import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.ActionHost
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.Glyph
import com.ekquickfind.core.search.ProviderId
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.Section
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZeroQueryStateTest {
    private object Open : Action {
        override val label = "Open"
        override val glyph: Glyph? = null

        override suspend fun perform(host: ActionHost) = ActionOutcome.Close
    }

    private val camera =
        SearchResult(ResultId("apps:Camera"), ProviderId("apps"), Section.Apps, "Camera", null, ResultIcon.Vector(Glyph.Search), 1f, Open)

    @Test
    fun recentsInTheStripLeaveNothingForThePanel() {
        assertFalse(ZeroQueryState(recent = listOf(camera)).showsPanel(recentsInStrip = true))
    }

    @Test
    fun recentsShowInThePanelWhenTheStripIsOff() {
        assertTrue(ZeroQueryState(recent = listOf(camera)).showsPanel(recentsInStrip = false))
    }

    @Test
    fun pinnedItemsKeepThePanelWithTheStripOn() {
        assertTrue(ZeroQueryState(pinned = listOf(camera), recent = listOf(camera)).showsPanel(recentsInStrip = true))
    }

    @Test
    fun suggestedAppsKeepThePanelWithTheStripOn() {
        assertTrue(ZeroQueryState(suggested = listOf(camera)).showsPanel(recentsInStrip = true))
    }

    @Test
    fun anEmptyHistoryShowsThePanelWithTheHint() {
        assertTrue(ZeroQueryState().showsPanel(recentsInStrip = true))
    }
}
