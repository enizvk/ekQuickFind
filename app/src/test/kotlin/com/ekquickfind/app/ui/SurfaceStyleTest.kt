package com.ekquickfind.app.ui

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ekquickfind.app.search.engineConfigFor
import com.ekquickfind.app.ui.theme.SurfaceStyle
import com.ekquickfind.app.ui.theme.toSurfaceStyle
import com.ekquickfind.app.ui.theme.withResultFont
import com.ekquickfind.core.search.Section
import com.ekquickfind.data.prefs.ActiveItemStyle
import com.ekquickfind.data.prefs.BarAnimation
import com.ekquickfind.data.prefs.Density
import com.ekquickfind.data.prefs.IconSize
import com.ekquickfind.data.prefs.ItemAnimation
import com.ekquickfind.data.prefs.ResultFont
import com.ekquickfind.data.prefs.ResultWeight
import com.ekquickfind.data.prefs.SectionKey
import com.ekquickfind.data.prefs.SectionLayout
import com.ekquickfind.data.prefs.TopResultStyle
import com.ekquickfind.data.prefs.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun barAnimationIsTwinLightsByDefault() {
        assertEquals(BarAnimation.TwinLights, UserPreferences().toSurfaceStyle().barAnimation)
        assertEquals(BarAnimation.ColourPulse, UserPreferences(barAnimation = BarAnimation.ColourPulse).toSurfaceStyle().barAnimation)
    }

    @Test
    fun theBarAndItemAnimationsTurnOffSeparately() {
        val barOff = UserPreferences(barAnimationOn = false).toSurfaceStyle()
        assertNull(barOff.barAnimation)
        assertEquals(ItemAnimation.Settle, barOff.itemAnimation)
        val itemOff = UserPreferences(itemAnimationOn = false).toSurfaceStyle()
        assertNull(itemOff.itemAnimation)
        assertEquals(BarAnimation.TwinLights, itemOff.barAnimation)
    }

    @Test
    fun reduceMotionStopsBothAnimations() {
        val style = UserPreferences(reduceMotion = true).toSurfaceStyle()
        assertNull(style.barAnimation)
        assertNull(style.itemAnimation)
    }

    @Test
    fun activeItemStyleDefaultsToTintedCardAndStaysWithAnimationsOff() {
        assertEquals(ActiveItemStyle.TintedCard, UserPreferences().toSurfaceStyle().activeItem)
        assertEquals(
            ActiveItemStyle.Gradient,
            UserPreferences(activeItemStyle = ActiveItemStyle.Gradient, itemAnimationOn = false).toSurfaceStyle().activeItem,
        )
    }

    @Test
    fun textScaleFollowsTheSlider() {
        assertEquals(1f, UserPreferences().toSurfaceStyle().textScale, 0f)
        assertEquals(1.3f, UserPreferences(textScale = 1.3f).toSurfaceStyle().textScale, 0f)
    }

    @Test
    fun defaultFontKeepsTheThemeFontAndOthersEachGetTheirOwn() {
        assertNull(UserPreferences().toSurfaceStyle().resultFont)
        val families =
            ResultFont.entries.filter { it != ResultFont.Default }.map {
                UserPreferences(
                    resultFont = it,
                ).toSurfaceStyle().resultFont
            }
        assertTrue(families.all { it != null })
        assertEquals(families.size, families.toSet().size)
        assertTrue(UserPreferences(resultFont = ResultFont.Rounded).toSurfaceStyle().resultFont != null)
    }

    @Test
    fun aResultFontAppliesToEveryTextStyle() {
        val typography =
            androidx.compose.material3
                .Typography()
                .withResultFont(androidx.compose.ui.text.font.FontFamily.Serif, null)
        listOf(
            typography.titleLarge,
            typography.bodyLarge,
            typography.bodyMedium,
            typography.bodySmall,
            typography.labelLarge,
            typography.labelSmall,
        ).forEach { assertEquals(androidx.compose.ui.text.font.FontFamily.Serif, it.fontFamily) }
    }

    @Test
    fun aResultWeightAppliesToEveryTextStyleAndKeepsTheFont() {
        val base = androidx.compose.material3.Typography()
        val bold = base.withResultFont(null, FontWeight.Bold)
        listOf(bold.titleLarge, bold.bodyLarge, bold.labelSmall).forEach { assertEquals(FontWeight.Bold, it.fontWeight) }
        assertEquals(base.bodyLarge.fontFamily, bold.bodyLarge.fontFamily)
    }

    @Test
    fun regularWeightKeepsTheThemeWeightsAndOthersGrowLightToBold() {
        assertNull(UserPreferences().toSurfaceStyle().resultWeight)
        val weights =
            listOf(ResultWeight.Light, ResultWeight.Medium, ResultWeight.Bold).map {
                UserPreferences(resultWeight = it).toSurfaceStyle().resultWeight!!
            }
        assertEquals(weights.sorted(), weights)
    }

    @Test
    fun theTopResultStaysInItsSectionUnlessFeatured() {
        assertTrue(UserPreferences().toSurfaceStyle().inlineTop)
        assertFalse(UserPreferences(topResult = TopResultStyle.Featured).toSurfaceStyle().inlineTop)
    }

    @Test
    fun theExactMatchButtonShowsUnlessHidden() {
        assertTrue(UserPreferences().toSurfaceStyle().exactMatchButton)
        assertFalse(UserPreferences(exactMatchButton = false).toSurfaceStyle().exactMatchButton)
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
