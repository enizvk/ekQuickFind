package com.ekquickfind.app.ui.settings

import com.ekquickfind.data.prefs.ActiveItemStyle
import com.ekquickfind.data.prefs.BackgroundStyle
import com.ekquickfind.data.prefs.CustomSite
import com.ekquickfind.data.prefs.Density
import com.ekquickfind.data.prefs.ThemeMode
import com.ekquickfind.data.prefs.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendlierSettingsTest {
    // Panels

    @Test
    fun nothingIsMarkedChangedOnAFreshInstall() {
        Panel.entries.forEach { assertFalse("${it.title} changed", it.isChanged(UserPreferences())) }
    }

    @Test
    fun aChangedOptionMarksOnlyItsPanel() {
        val p = UserPreferences(blurRadiusDp = 50)
        assertEquals(listOf(Panel.Background), Panel.entries.filter { it.isChanged(p) })
    }

    @Test
    fun resettingAPanelPutsBackOnlyItsOwnOptions() {
        val p = UserPreferences(blurRadiusDp = 50, backgroundStyle = BackgroundStyle.Solid, theme = ThemeMode.Dark)
        val reset = Panel.Background.reset(p)
        assertEquals(UserPreferences().blurRadiusDp, reset.blurRadiusDp)
        assertEquals(UserPreferences().backgroundStyle, reset.backgroundStyle)
        assertEquals(ThemeMode.Dark, reset.theme)
    }

    @Test
    fun resettingSearchSitesKeepsTheSitesYouAdded() {
        val mine = CustomSite("c", "Example", "ex", "https://example.com/?q=%s")
        val p = UserPreferences(siteSeparator = ":", customSites = listOf(mine))
        val reset = Panel.SearchSites.reset(p)
        assertEquals("", reset.siteSeparator)
        assertEquals(listOf(mine), reset.customSites)
    }

    // Quick looks

    @Test
    fun minimalIsSolidStillAndCompact() {
        val p = UserPreferences().withLook(LookPreset.Minimal)
        assertEquals(BackgroundStyle.Solid, p.backgroundStyle)
        assertFalse(p.barAnimationOn)
        assertFalse(p.itemAnimationOn)
        assertEquals(Density.Compact, p.density)
    }

    @Test
    fun balancedIsTheDefaultLookAndShowcaseTheFanciest() {
        assertTrue(LookPreset.Balanced.isApplied(UserPreferences()))
        val showcase = UserPreferences().withLook(LookPreset.Showcase)
        assertEquals(BackgroundStyle.FrostedGlass, showcase.backgroundStyle)
        assertEquals(ActiveItemStyle.Gradient, showcase.activeItemStyle)
        assertTrue(showcase.barAnimationOn && showcase.itemAnimationOn)
        assertTrue(LookPreset.Showcase.isApplied(showcase))
    }

    @Test
    fun aLookOnlyChangesHowThingsLook() {
        val p = UserPreferences(theme = ThemeMode.Dark, recentsCount = 9, textScale = 1.3f).withLook(LookPreset.Minimal)
        assertEquals(ThemeMode.Dark, p.theme)
        assertEquals(9, p.recentsCount)
        assertEquals(1.3f, p.textScale, 0f)
    }

    // Settings search

    @Test
    fun searchingFindsTheRightPanel() {
        assertEquals(Panel.Background, SettingsIndex.search("blur").first().panel)
        assertEquals(Panel.Text, SettingsIndex.search("font").first().panel)
        assertEquals(Panel.SearchSites, SettingsIndex.search("keyword").first().panel)
        assertEquals(Panel.FloatingButton, SettingsIndex.search("floating").first().panel)
    }

    @Test
    fun searchIgnoresCaseAndFindsWordsInsideTitles() {
        assertEquals(Panel.Background, SettingsIndex.search("BLUR").first().panel)
        assertTrue(SettingsIndex.search("separator").any { it.panel == Panel.SearchSites })
    }

    @Test
    fun anEmptyOrUnknownSearchFindsNothing() {
        assertTrue(SettingsIndex.search("").isEmpty())
        assertTrue(SettingsIndex.search("zzqx").isEmpty())
    }

    @Test
    fun everyEntryPointsAtAPanelOnItsPage() {
        SettingsIndex.entries.forEach { assertEquals(it.title, it.panel.page, it.page) }
    }
}
