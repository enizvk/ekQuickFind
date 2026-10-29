package com.ekquickfind.app.settings

import com.ekquickfind.providers.apps.AppEntry
import com.ekquickfind.providers.apps.AppsProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class ExcludedAppsTest {
    private val calc = AppEntry("com.calc", "com.calc.Main", "Calculator", 0, null, true)
    private val slack = AppEntry("com.slack", "com.slack.Main", "Slack", 10, "Work", false)
    private val maps = AppEntry("com.maps", "com.maps.Main", "maps", 0, null, false)

    @Test
    fun listsEveryAppSortedByLabelWithItsExclusionState() {
        val rows = excludableApps(listOf(slack, calc, maps), hidden = setOf(AppsProvider.resultId(slack).value), filter = "")
        assertEquals(listOf("Calculator", "maps", "Slack"), rows.map { it.label })
        assertEquals(listOf(false, false, true), rows.map { it.excluded })
        assertEquals("Work", rows.last().badge)
    }

    @Test
    fun filterMatchesLabelsLoosely() {
        assertEquals(listOf("Calculator"), excludableApps(listOf(slack, calc, maps), emptySet(), filter = "calc").map { it.label })
        assertEquals(listOf("Slack"), excludableApps(listOf(slack, calc, maps), emptySet(), filter = "SLA").map { it.label })
    }

    @Test
    fun excludedAppsSortFirstWhenAskedSoTheyAreEasyToFind() {
        val rows = excludableApps(listOf(slack, calc, maps), setOf(AppsProvider.resultId(maps).value), filter = "", excludedFirst = true)
        assertEquals("maps", rows.first().label)
    }
}
