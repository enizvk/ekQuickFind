package com.ekquickfind.app.ui

import androidx.compose.ui.unit.dp
import com.ekquickfind.app.search.engineConfigFor
import com.ekquickfind.app.ui.theme.SurfaceStyle
import com.ekquickfind.app.ui.theme.toSurfaceStyle
import com.ekquickfind.core.search.Section
import com.ekquickfind.data.prefs.Density
import com.ekquickfind.data.prefs.IconSize
import com.ekquickfind.data.prefs.SectionKey
import com.ekquickfind.data.prefs.SectionLayout
import com.ekquickfind.data.prefs.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SurfaceStyleTest {
    @Test
    fun webAlwaysShowsAsALinkListEvenIfAGridWasSaved() {
        val style = UserPreferences(sectionLayouts = mapOf(SectionKey.Web to SectionLayout.Grid)).toSurfaceStyle()
        assertEquals(SectionLayout.Rows, style.layoutOf(Section.Web))
    }

    @Test
    fun webRowsAreDenseWithSmallerIconsAtEverySize() {
        IconSize.entries.forEach { size ->
            val style = UserPreferences(iconSize = size).toSurfaceStyle()
            assertTrue("$size icon", style.denseIconSize < style.iconSize)
            assertTrue("$size padding", style.denseRowPadding < style.rowPadding)
        }
        val style = UserPreferences().toSurfaceStyle()
        assertTrue(style.isDense(Section.Web))
        Section.entries.filter { it != Section.Web }.forEach { assertTrue("$it dense", !style.isDense(it)) }
    }

    @Test
    fun iconSizesGrowSmallToLarge() {
        val sizes = IconSize.entries.map { UserPreferences(iconSize = it).toSurfaceStyle().iconSize }
        assertEquals(sizes.sorted(), sizes)
        assertEquals(40.dp, UserPreferences().toSurfaceStyle().iconSize)
    }

    @Test
    fun compactDensityTightensRows() {
        assertTrue(UserPreferences(density = Density.Compact).toSurfaceStyle().rowPadding < UserPreferences().toSurfaceStyle().rowPadding)
    }

    @Test
    fun layoutsMapToEngineSections() {
        val style = UserPreferences(sectionLayouts = mapOf(SectionKey.Settings to SectionLayout.Chips)).toSurfaceStyle()
        assertEquals(SectionLayout.Chips, style.layoutOf(Section.Settings))
        assertEquals(SectionLayout.Grid, style.layoutOf(Section.Apps))
        assertEquals(SectionLayout.Chips, style.recentLayout)
    }

    @Test
    fun engineConfigFollowsOrderAndResultsPerSection() {
        val config =
            engineConfigFor(
                UserPreferences(resultsPerSection = 6, sectionOrder = listOf(SectionKey.Settings, SectionKey.Apps)),
                gridColumns = 5,
            )
        assertEquals(listOf(Section.Settings, Section.Apps), config.orderedSections.take(2))
        assertEquals(6, config.capFor(Section.Files))
        // Grid sections show two full rows before "More".
        assertEquals(10, config.capFor(Section.Apps))
    }

    @Test
    fun defaultStyleIsTheDefaultPreferences() = assertEquals(SurfaceStyle(), UserPreferences().toSurfaceStyle())
}
