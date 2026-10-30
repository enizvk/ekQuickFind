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
import com.ekquickfind.data.history.RecentResult
import org.junit.Assert.assertEquals
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

    private fun app(name: String) = camera.copy(id = ResultId("apps:$name"), title = name)

    @Test
    fun onlyTheChosenNumberOfRecentsShowNewestFirst() {
        val recents = (1..8).map { app("App $it") }
        val state = ZeroQueryState(pinned = listOf(camera), recent = recents).limitRecents(5)
        assertEquals(recents.take(5), state.recent)
        assertEquals(listOf(camera), state.pinned)
    }

    @Test
    fun smartPicksComeFirstThenUsageAppsWithoutRepeatsOrShownItems() {
        val a = app("A")
        val b = app("B")
        val c = app("C")
        val d = app("D")
        val rows = suggestedRows(smart = listOf(b, a), usage = listOf(a, c, d), shown = setOf(d.id), limit = 3)
        assertEquals(listOf(b, a, c), rows)
    }

    @Test
    fun recentsInTheStripLeaveOnlyTheHintInThePanel() {
        assertTrue(ZeroQueryState(recent = listOf(camera)).onlyHint(recentsInStrip = true))
    }

    @Test
    fun recentsShowInThePanelWhenTheStripIsOff() {
        assertFalse(ZeroQueryState(recent = listOf(camera)).onlyHint(recentsInStrip = false))
    }

    @Test
    fun pinnedItemsFillThePanelWithTheStripOn() {
        assertFalse(ZeroQueryState(pinned = listOf(camera), recent = listOf(camera)).onlyHint(recentsInStrip = true))
    }

    @Test
    fun suggestedAppsFillThePanelWithTheStripOn() {
        assertFalse(ZeroQueryState(suggested = listOf(camera)).onlyHint(recentsInStrip = true))
    }

    @Test
    fun aNextUpEventFillsThePanelAndIsNotEmpty() {
        val state = ZeroQueryState(nextUp = camera)
        assertFalse(state.isEmpty)
        assertFalse(state.onlyHint(recentsInStrip = true))
    }

    @Test
    fun anEmptyHistoryShowsTheHint() {
        assertTrue(ZeroQueryState().onlyHint(recentsInStrip = true))
        assertTrue(ZeroQueryState().onlyHint(recentsInStrip = false))
    }

    @Test
    fun clearingRecentsHidesWhatWasOpenedBeforeButNotAfter() {
        val before = RecentResult("apps:Camera", "apps", lastAt = 100)
        val after = RecentResult("apps:Maps", "apps", lastAt = 300)
        assertEquals(listOf(after), visibleRecents(listOf(before, after), clearedAt = 200))
        assertEquals(listOf(before, after), visibleRecents(listOf(before, after), clearedAt = 0))
    }
}
