package com.ekquickfind.providers.sites

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class SiteCatalogTest {
    private val none: (String) -> Boolean = { false }
    private val all: (String) -> Boolean = { true }

    @Test
    fun youtubeSearchesInItsAppWhenInstalledElseOnTheWeb() {
        assertEquals(
            SiteOpen.AppSearch("com.google.android.youtube", "android.intent.action.SEARCH", "query", "lofi beats"),
            SiteCatalog.YouTube.open("lofi beats", all, Locale.UK),
        )
        assertEquals(
            SiteOpen.Link("https://www.youtube.com/results?search_query=lofi+beats"),
            SiteCatalog.YouTube.open("lofi beats", none, Locale.UK),
        )
    }

    @Test
    fun translateGoesToTheAppOrTranslatesIntoThePhoneLanguage() {
        assertEquals(
            SiteOpen.AppSearch(
                "com.google.android.apps.translate",
                "android.intent.action.SEND",
                "android.intent.extra.TEXT",
                "dobro utro",
                "text/plain",
            ),
            SiteCatalog.Translate.open("dobro utro", all, Locale.UK),
        )
        assertEquals(
            SiteOpen.Link("https://translate.google.com/?sl=auto&tl=de&text=guten+morgen&op=translate"),
            SiteCatalog.Translate.open("guten morgen", none, Locale.GERMANY),
        )
    }

    @Test
    fun mapsAndPlayUseTheirAppLinksWhenInstalled() {
        assertEquals(SiteOpen.Link("geo:0,0?q=pizza+near+me"), SiteCatalog.Maps.open("pizza near me", all, Locale.UK))
        assertEquals(
            SiteOpen.Link("https://www.google.com/maps/search/?api=1&query=pizza"),
            SiteCatalog.Maps.open("pizza", none, Locale.UK),
        )
        assertEquals(SiteOpen.Link("market://search?q=calculator&c=apps"), SiteCatalog.Play.open("calculator", all, Locale.UK))
        assertEquals(
            SiteOpen.Link("https://play.google.com/store/search?q=calculator&c=apps"),
            SiteCatalog.Play.open("calculator", none, Locale.UK),
        )
    }

    @Test
    fun wikipediaUsesThePhoneLanguageAndRedditItsSearch() {
        assertEquals(
            SiteOpen.Link("https://bg.wikipedia.org/w/index.php?search=Andromeda"),
            SiteCatalog.Wikipedia.open("Andromeda", all, Locale.forLanguageTag("bg-BG")),
        )
        assertEquals(
            SiteOpen.Link("https://www.reddit.com/search/?q=android+tips"),
            SiteCatalog.Reddit.open("android tips", all, Locale.UK),
        )
    }

    @Test
    fun aCustomSiteFillsItsTemplate() {
        assertEquals(
            SiteOpen.Link("https://example.com/find?q=caf%C3%A9+%26+bar"),
            customOpen("https://example.com/find?q=%s", "café & bar"),
        )
    }

    @Test
    fun onlyHttpsTemplatesWithAPlaceholderAreValid() {
        assertEquals(true, isValidTemplate("https://example.com/?q=%s"))
        assertEquals(false, isValidTemplate("http://example.com/?q=%s"))
        assertEquals(false, isValidTemplate("https://example.com/"))
        assertEquals(false, isValidTemplate("javascript:%s"))
    }

    @Test
    fun keywordsAreShortLettersAndDigits() {
        assertEquals(true, isValidKeyword("yt"))
        assertEquals(true, isValidKeyword("wiki2"))
        assertEquals(false, isValidKeyword(""))
        assertEquals(false, isValidKeyword("y t"))
        assertEquals(false, isValidKeyword("yt:"))
        assertEquals(false, isValidKeyword("averyveryverylongkeyword"))
    }
}
