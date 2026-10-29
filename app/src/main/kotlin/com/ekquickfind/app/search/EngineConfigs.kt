package com.ekquickfind.app.search

import com.ekquickfind.app.ui.theme.key
import com.ekquickfind.app.ui.theme.section
import com.ekquickfind.core.search.EngineConfig
import com.ekquickfind.core.search.Section
import com.ekquickfind.data.prefs.SectionLayout
import com.ekquickfind.data.prefs.UserPreferences

/** Engine settings derived from preferences. Grid sections get two full rows of tiles before "More". */
fun engineConfigFor(
    prefs: UserPreferences,
    gridColumns: Int,
): EngineConfig {
    val caps =
        Section.entries
            .filter { prefs.layoutFor(it.key()) == SectionLayout.Grid }
            .associateWith { gridColumns * 2 }
    return EngineConfig(
        sectionCap = prefs.resultsPerSection,
        sectionCaps = caps + (Section.QuickAnswers to 1),
        sectionOrder = prefs.sectionOrder.mapNotNull { it.section() },
    )
}
