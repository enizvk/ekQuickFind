package com.ekquickfind.providers.web

import com.ekquickfind.data.prefs.SearchEngineChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestEngineTest {
    private fun fixture(name: String) = javaClass.classLoader!!.getResource("web/$name.json")!!.readText()

    @Test
    fun parsesRecordedResponsesFromEveryEngine() {
        mapOf(
            "duckduckgo" to "weather in athens",
            "google" to "weather in sofia",
            "bing" to "weather in sofia",
            "brave" to "weather in london 10 days",
        ).forEach { (engine, first) ->
            val parsed = OpenSearchSuggestions.parse(fixture(engine))
            assertEquals("$engine first", first, parsed.first())
            assertTrue("$engine count", parsed.size >= 5)
        }
    }

    @Test
    fun garbageParsesToNothing() {
        listOf("", "{}", "<html>", "[\"q\"]", "[1, 2]").forEach { assertTrue(it, OpenSearchSuggestions.parse(it).isEmpty()) }
    }

    @Test
    fun builtInEnginesEncodeTheQuery() {
        val ddg = WebEngine.forChoice(SearchEngineChoice.DuckDuckGo)
        assertEquals("https://duckduckgo.com/?q=caf%C3%A9+%26+bar", ddg.searchUrl("café & bar"))
        assertEquals("https://duckduckgo.com/ac/?q=caf%C3%A9+%26+bar&type=list", ddg.suggestUrl("café & bar"))
        assertTrue(WebEngine.forChoice(SearchEngineChoice.Google).searchUrl("x").startsWith("https://www.google.com/search?q="))
        assertTrue(WebEngine.forChoice(SearchEngineChoice.Bing).suggestUrl("x")!!.startsWith("https://api.bing.com/osjson.aspx?query="))
        assertTrue(WebEngine.forChoice(SearchEngineChoice.Brave).suggestUrl("x")!!.startsWith("https://search.brave.com/api/suggest?q="))
    }

    @Test
    fun customTemplatesSubstituteTheQuery() {
        val custom = WebEngine.forChoice(SearchEngineChoice.Custom, "https://example.com/find?q=%s", "https://example.com/ac?q=%s")
        assertEquals("https://example.com/find?q=cats+dogs", custom.searchUrl("cats dogs"))
        assertEquals("https://example.com/ac?q=cats+dogs", custom.suggestUrl("cats dogs"))
    }

    @Test
    fun customWithoutASuggestionTemplateHasNoSuggestions() {
        assertNull(WebEngine.forChoice(SearchEngineChoice.Custom, "https://example.com/find?q=%s", null).suggestUrl("cats"))
    }

    @Test
    fun customWithoutAValidTemplateFallsBackToDuckDuckGo() {
        assertTrue(
            WebEngine
                .forChoice(
                    SearchEngineChoice.Custom,
                    "https://example.com/no-placeholder",
                    null,
                ).searchUrl("x")
                .startsWith("https://duckduckgo.com/"),
        )
    }
}
