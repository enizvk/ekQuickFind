package com.ekquickfind.core.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class HighlightTest {
    /** Renders highlights as [brackets] for readable assertions. */
    private fun marked(
        query: String,
        text: String,
    ): String {
        val ranges = QueryMatcher.highlight(TextNormalizer.normalize(query), MatchTarget(text))
        val sb = StringBuilder()
        text.forEachIndexed { i, c ->
            if (ranges.any { it.first == i }) sb.append('[')
            sb.append(c)
            if (ranges.any { it.last == i }) sb.append(']')
        }
        return sb.toString()
    }

    @Test
    fun `prefix is highlighted`() = assertEquals("[Notif]ication", marked("notif", "Notification"))

    @Test
    fun `word prefix is highlighted`() = assertEquals("My [Notif]ication", marked("notif", "My Notification"))

    @Test
    fun `initials highlight the first letter of each word`() = assertEquals("[V]isual [S]tudio [C]ode", marked("vsc", "Visual Studio Code"))

    @Test
    fun `all words highlights each matched word prefix`() = assertEquals("[Goo]gle [Maps]", marked("maps goo", "Google Maps"))

    @Test
    fun `substring is highlighted`() = assertEquals("Go[ogle]", marked("ogle", "Google"))

    @Test
    fun `typo highlights the matched span`() = assertEquals("[WhatsApp]", marked("watsapp", "WhatsApp"))

    @Test
    fun `accented characters stay inside the highlight`() = assertEquals("[Café] Finder", marked("cafe", "Café Finder"))

    @Test
    fun `decomposed accents keep their combining mark highlighted`() = assertEquals("[Café] Finder", marked("cafe", "Café Finder"))

    @Test
    fun `extra spaces in the text don't shift the highlight`() = assertEquals("  Google   [Maps]", marked("maps", "  Google   Maps"))

    @Test
    fun `completion emphasis marks what follows the typed text`() {
        fun completion(
            query: String,
            text: String,
        ): String {
            val ranges = QueryMatcher.completion(TextNormalizer.normalize(query), MatchTarget(text))
            val sb = StringBuilder()
            text.forEachIndexed { i, c ->
                if (ranges.any { it.first == i }) sb.append('[')
                sb.append(c)
                if (ranges.any { it.last == i }) sb.append(']')
            }
            return sb.toString()
        }
        assertEquals("sound[cloud]", completion("sound", "soundcloud"))
        assertEquals("sound [effects]", completion("sound", "sound effects"))
        assertEquals("[best] sound [bars]", completion("sound", "best sound bars"))
        assertEquals("sound", completion("sound", "sound"))
    }

    @Test
    fun `no match means no highlight`() = assertTrue(QueryMatcher.highlight("zzz", MatchTarget("Camera")).isEmpty())

    @ParameterizedTest
    @ValueSource(strings = ["Café", "  a  b ", "İstanbul", "Straße", "日本語 テキスト", "", "é́"])
    fun `index map reproduces the normaliser exactly`(raw: String) {
        assertEquals(TextNormalizer.normalize(raw), MatchTarget(raw).normalized)
    }
}
