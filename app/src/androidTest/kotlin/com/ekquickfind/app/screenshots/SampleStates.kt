package com.ekquickfind.app.screenshots

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

/** Deterministic states for screenshot tests (vector icons only, so output doesn't depend on installed apps). */
object SampleStates {
    private object Noop : Action {
        override val label = "Open"
        override val glyph: Glyph? = null

        override suspend fun perform(host: ActionHost) = ActionOutcome.Close
    }

    private fun row(
        title: String,
        section: Section,
        glyph: Glyph,
        subtitle: String? = null,
        badge: String? = null,
    ) = RankedResult(
        SearchResult(
            id = ResultId("sample:$title"),
            providerId = ProviderId(section.name),
            section = section,
            title = title,
            subtitle = subtitle,
            icon = ResultIcon.Vector(glyph),
            score = 0.5f,
            primaryAction = Noop,
            badge = badge,
        ),
        0.5f,
    )

    val mixed =
        SearchState(
            query = Query("ma"),
            top = row("Maps", Section.Apps, Glyph.Search, subtitle = "Top result"),
            sections =
                listOf(
                    SectionResults(
                        Section.Apps,
                        listOf(row("Mail", Section.Apps, Glyph.Message), row("Magnifier", Section.Apps, Glyph.Search, badge = "Work")),
                        cap = 4,
                    ),
                    SectionResults(
                        Section.Settings,
                        listOf(row("Manage storage", Section.Settings, Glyph.Settings, "Battery and device care")),
                        cap = 4,
                    ),
                    SectionResults(
                        Section.Files,
                        listOf(row("march-report.pdf", Section.Files, Glyph.Document, "Downloads · 2 days ago")),
                        cap = 4,
                    ),
                    SectionResults(Section.Web, listOf(row("Search the web for ma", Section.Web, Glyph.Web)), cap = 4),
                ),
            isSettled = true,
        )

    val loading =
        SearchState(
            query = Query("m"),
            top = null,
            sections = listOf(SectionResults(Section.Apps, emptyList(), cap = 4, isLoading = true)),
            isSettled = false,
        )
}
