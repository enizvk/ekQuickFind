package com.ekquickfind.app

import com.ekquickfind.data.prefs.SearchEngineChoice
import com.ekquickfind.data.prefs.SectionKey
import com.ekquickfind.data.prefs.SectionLayout
import com.ekquickfind.data.prefs.TopResultStyle
import com.ekquickfind.data.prefs.UserPreferences

/**
 * What UI tests start from: the defaults, pinned to the layout the tests were written for (big top-result card,
 * settings as rows, DuckDuckGo, no floating button), so changing a default doesn't silently change every test.
 */
fun testBaseline(current: UserPreferences): UserPreferences =
    current.resetToDefaults().copy(
        topResult = TopResultStyle.Featured,
        sectionLayouts = mapOf(SectionKey.Settings to SectionLayout.Rows),
        searchEngine = SearchEngineChoice.DuckDuckGo,
        floatingButton = false,
    )
