package com.ekquickfind.app.ui.search

import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.ActionHost
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.Glyph
import com.ekquickfind.core.search.ProviderId
import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.RankedResult
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.SearchState
import com.ekquickfind.core.search.Section
import com.ekquickfind.core.search.SectionResults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class InlineTopTest {
    private object Open : Action {
        override val label = "Open"
        override val glyph: Glyph? = null

        override suspend fun perform(host: ActionHost) = ActionOutcome.Close
    }

    private fun ranked(
        title: String,
        section: Section,
    ) = RankedResult(
        SearchResult(ResultId("t:$title"), ProviderId("t"), section, title, null, ResultIcon.Vector(Glyph.Search), 1f, Open),
        1f,
    )

    private val maps = ranked("Maps", Section.Apps)
    private val mail = ranked("Mail", Section.Apps)
    private val wifi = ranked("Wi-Fi", Section.Settings)

    private fun state(
        top: RankedResult?,
        vararg sections: SectionResults,
    ) = SearchState(Query("ma"), top, sections.toList(), isSettled = true)

    @Test
    fun theTopResultMovesToTheFrontOfItsSection() {
        val inline =
            state(
                maps,
                SectionResults(Section.Apps, listOf(mail), cap = 4),
                SectionResults(Section.Settings, listOf(wifi), cap = 4),
            ).withTopInline()
        assertNull(inline.top)
        assertEquals(listOf(maps, mail), inline.sections[0].items)
        assertEquals(listOf(wifi), inline.sections[1].items)
    }

    @Test
    fun aTopResultAloneInItsSectionGetsThatSectionFirst() {
        val inline = state(maps, SectionResults(Section.Settings, listOf(wifi), cap = 4)).withTopInline()
        assertEquals(listOf(Section.Apps, Section.Settings), inline.sections.map { it.section })
        assertEquals(listOf(maps), inline.sections[0].items)
    }

    @Test
    fun withoutATopResultNothingChanges() {
        val original = state(null, SectionResults(Section.Settings, listOf(wifi), cap = 4))
        assertSame(original, original.withTopInline())
    }
}
