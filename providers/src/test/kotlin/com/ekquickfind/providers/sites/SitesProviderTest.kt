package com.ekquickfind.providers.sites

import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.Section
import com.ekquickfind.data.prefs.CustomSite
import com.ekquickfind.data.prefs.SiteState
import com.ekquickfind.data.prefs.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class SitesProviderTest {
    private fun provider(
        prefs: UserPreferences = UserPreferences(),
        installed: Set<String> = setOf("com.google.android.youtube"),
    ) = SitesProvider(
        config = { SitesConfig.from(prefs) },
        isInstalled = { it in installed },
        locale = { Locale.UK },
    )

    @Test
    fun aKeywordMakesTheSiteTheBestMatchOpenedInItsApp() =
        runTest {
            val row = provider().search(Query("yt cats")).first().single()
            assertEquals("Search YouTube for “cats”", row.title)
            assertEquals(Section.Sites, row.section)
            assertEquals(1f, row.score)
            assertEquals("com.google.android.youtube", row.handler)
        }

    @Test
    fun aMatchingWordGivesALowerRow() =
        runTest {
            val row = provider().search(Query("translate good morning")).first().single()
            assertEquals("Translate “good morning”", row.title)
            assertTrue(row.score < 1f)
        }

    @Test
    fun renamedKeywordsAndTurnedOffSitesAreRespected() =
        runTest {
            val prefs = UserPreferences(sites = mapOf("youtube" to SiteState(keyword = "tube"), "reddit" to SiteState(enabled = false)))
            assertEquals(
                "Search YouTube for “cats”",
                provider(prefs)
                    .search(Query("tube cats"))
                    .first()
                    .single()
                    .title,
            )
            assertTrue(provider(prefs).search(Query("yt cats")).first().isEmpty())
            assertTrue(provider(prefs).search(Query("r android")).first().isEmpty())
        }

    @Test
    fun theChosenSeparatorIsUsed() =
        runTest {
            val dash = UserPreferences(siteSeparator = "-")
            assertEquals(
                "Find “pizza” on Maps",
                provider(dash)
                    .search(Query("map - pizza"))
                    .first()
                    .single()
                    .title,
            )
        }

    @Test
    fun aCustomSiteGetsItsOwnRow() =
        runTest {
            val prefs = UserPreferences(customSites = listOf(CustomSite("c1", "Example", "ex", "https://example.com/?q=%s")))
            val row = provider(prefs).search(Query("ex hello")).first().single()
            assertEquals("Search Example for “hello”", row.title)
        }

    @Test
    fun weatherIsLeftToTheWeatherCard() =
        runTest {
            assertTrue(provider().search(Query("w sofia")).first().isEmpty())
            assertTrue(provider().search(Query("weather sofia")).first().isEmpty())
        }

    @Test
    fun ordinarySearchesGetNothing() =
        runTest {
            assertTrue(provider().search(Query("camera")).first().isEmpty())
        }

    @Test
    fun theBarHintNamesTheSiteAndHowMuchToColour() {
        val config = SitesConfig.from(UserPreferences(siteSeparator = ":", sites = mapOf("weather" to SiteState(enabled = true))))
        assertEquals(KeywordHint("YouTube", com.ekquickfind.core.search.Glyph.Video, end = 3), config.hint("yt: cats"))
        assertEquals(KeywordHint("Weather", com.ekquickfind.core.search.Glyph.Weather, end = 1), config.hint("w sofia"))
        assertEquals(null, config.hint("youtube cats"))
    }

    @Test
    fun redditWikipediaAndWeatherStartOff() =
        runTest {
            assertTrue(provider().search(Query("r android")).first().isEmpty())
            assertTrue(provider().search(Query("wiki cats")).first().isEmpty())
            val config = SitesConfig.from(UserPreferences())
            assertEquals(null, config.weatherKeyword)
            assertEquals(null, config.hint("w sofia"))
        }

    @Test
    fun anOffByDefaultSiteWorksOnceTurnedOn() =
        runTest {
            val prefs = UserPreferences(sites = mapOf("reddit" to SiteState(enabled = true)))
            assertEquals(
                "Search Reddit for “android”",
                provider(prefs)
                    .search(Query("r android"))
                    .first()
                    .single()
                    .title,
            )
        }

    @Test
    fun onlyRedditWikipediaAndWeatherAreOffWhenUntouched() {
        val off = (SiteCatalog.linkSites.map { it.id } + SiteCatalog.WEATHER_ID).filterNot { SiteCatalog.isOn(emptyMap(), it) }
        assertEquals(setOf("reddit", "wikipedia", "weather"), off.toSet())
        assertTrue(SiteCatalog.isOn(mapOf("reddit" to SiteState(enabled = true)), "reddit"))
        assertTrue(!SiteCatalog.isOn(mapOf("youtube" to SiteState(enabled = false)), "youtube"))
    }

    @Test
    fun aRemovedReadyMadeSiteIsGoneUntilRestored() =
        runTest {
            val removed = UserPreferences(removedSites = setOf("youtube", "weather"), sites = mapOf("weather" to SiteState(enabled = true)))
            assertTrue(provider(removed).search(Query("yt cats")).first().isEmpty())
            assertEquals(null, SitesConfig.from(removed).weatherKeyword)
            assertEquals(null, SitesConfig.from(removed).hint("yt cats"))
        }
}
