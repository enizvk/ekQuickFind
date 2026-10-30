package com.ekquickfind.app.ui.settings

import com.ekquickfind.data.prefs.CustomSite
import com.ekquickfind.data.prefs.SiteState
import com.ekquickfind.data.prefs.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSitesTest {
    @Test
    fun everyKeywordInUseCountsExceptTheSiteBeingEdited() {
        val prefs =
            UserPreferences(
                sites = mapOf("youtube" to SiteState(keyword = "tube")),
                customSites = listOf(CustomSite("c", "Ex", "ex", "https://e.com/%s")),
            )
        val used = keywordsInUse(prefs)
        assertTrue("tube" in used && "ex" in used && "w" in used && "tr" in used)
        assertTrue("yt" !in used)
        assertTrue("tube" !in keywordsInUse(prefs, exceptId = "youtube"))
    }

    @Test
    fun aKeywordMustBeValidAndFree() {
        assertNull(keywordProblem("yt", emptySet()))
        assertEquals("Another site uses “YT”", keywordProblem("YT", setOf("yt")))
        assertEquals("Use 1 to 12 letters or digits", keywordProblem("y t", emptySet()))
    }

    @Test
    fun renamingAnOffByDefaultSiteKeepsItOff() {
        val prefs = UserPreferences().withSite("reddit") { it.copy(keyword = "rd") }
        assertEquals(SiteState(enabled = false, keyword = "rd"), prefs.sites["reddit"])
    }

    @Test
    fun translationLanguagesStartWithThePhonesThenGoByName() {
        val options = translateLanguageOptions(listOf("de", "bg", "en"), java.util.Locale.UK)
        assertEquals(TranslateLanguageOption("", "Phone language (English)"), options.first())
        assertEquals(listOf("Bulgarian", "English", "German"), options.drop(1).map { it.name })
        assertEquals("Bulgarian", translateLanguageName("bg", java.util.Locale.UK))
        assertEquals("Phone language (English)", translateLanguageName("", java.util.Locale.UK))
    }

    @Test
    fun removingAReadyMadeSiteFreesItsKeywordAndRestoringBringsItBack() {
        val removed = UserPreferences().withoutSite("maps")
        assertEquals(setOf("maps"), removed.removedSites)
        assertTrue("map" !in keywordsInUse(removed))
        assertEquals(emptySet<String>(), removed.withDefaultSites().removedSites)
    }
}
