package com.ekquickfind.core.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class QueryMatcherTest {
    private fun score(
        query: String,
        text: String,
    ) = QueryMatcher.score(TextNormalizer.normalize(query), MatchTarget(text))

    @Test
    fun `two letters match the initials of a multi-word name`() {
        assertNotNull(score("gm", "Google Maps"))
        assertNotNull(score("gm", "Gmail"))
    }

    @Test
    fun `two letters still don't match inside words`() {
        assertNull(score("oo", "Google Maps"))
        assertNull(score("gm", "Pigment"))
    }

    @Test
    fun `two-letter initials highlight the first letter of each word`() {
        val target = MatchTarget("Google Maps")
        assertEquals(listOf(0..0, 7..7), QueryMatcher.highlight("gm", target))
    }

    @Test
    fun `single letters match only the start of a word`() {
        assertNotNull(score("s", "Slack"))
        assertNotNull(score("s", "My Settings"))
        assertNull(score("s", "Pulse"))
        assertNull(score("ul", "Pulse"))
    }

    private fun exact(
        query: String,
        text: String,
    ) = QueryMatcher.score(TextNormalizer.normalize(query), MatchTarget(text), exact = true)

    @Test
    fun `exact match drops initials, all-words and typo matches`() {
        assertNull(exact("vsc", "Visual Studio Code"))
        assertNull(exact("gm", "Google Maps"))
        assertNull(exact("whatsapq", "WhatsApp"))
        assertNull(exact("studio visual", "Visual Studio Code"))
    }

    @Test
    fun `exact match finds the typed text as whole words`() {
        assertNotNull(exact("eren", "Eren Yılmaz"))
        assertNotNull(exact("eren", "Ali Eren"))
        assertNotNull(exact("eren", "eren_beach.jpg"))
        assertNotNull(exact("google maps", "Google Maps"))
    }

    @Test
    fun `exact match skips the typed text inside longer words`() {
        assertNull(exact("eren", "Erenay"))
        assertNull(exact("eren", "Reference"))
        assertNull(exact("gm", "Gmail"))
        assertNull(exact("ings", "Settings"))
    }

    @Test
    fun `exact match ranks a name that starts with the word higher`() {
        assertTrue(exact("eren", "Eren")!! > exact("eren", "Eren Yılmaz")!!)
        assertTrue(exact("eren", "Eren Yılmaz")!! > exact("eren", "Ali Eren")!!)
    }

    @Test
    fun `exact match still ignores case and accents but not short mid-word hits`() {
        assertNotNull(exact("CAFE", "Café"))
        assertNull(exact("ul", "Pulse"))
    }

    @Test
    fun `exact match highlights the whole word only`() {
        assertEquals(listOf(4..7), QueryMatcher.highlight("eren", MatchTarget("Ali Eren"), exact = true))
        assertEquals(emptyList<IntRange>(), QueryMatcher.highlight("eren", MatchTarget("Erenay"), exact = true))
        assertEquals(emptyList<IntRange>(), QueryMatcher.highlight("gm", MatchTarget("Google Maps"), exact = true))
    }

    @Test
    fun `initials work at three letters too`() {
        assertNotNull(score("vsc", "Visual Studio Code"))
    }

    @Test
    fun `substring starts at three letters`() {
        assertNotNull(score("ail", "Gmail"))
    }

    @Test
    fun `whatsap matches WhatsApp`() {
        assertNotNull(score("whatsap", "WhatsApp"))
    }

    @ParameterizedTest(name = "\"{0}\" typo-matches \"{1}\"")
    @CsvSource("watsapp, WhatsApp", "spotfy, Spotify", "calendra, Calendar", "telegarm, Telegram")
    fun `one-edit typos match for queries of four or more characters`(
        query: String,
        text: String,
    ) {
        assertNotNull(score(query, text))
    }

    @Test
    fun `no typo matching for short queries`() {
        assertNull(score("ma", "Me"))
        assertNull(score("mxp", "Maps"))
    }

    @Test
    fun `unrelated text does not match`() {
        assertNull(score("camera", "Calculator"))
    }

    @Test
    fun `match kinds are ordered exact over prefix over word prefix over initials over substring over typo`() {
        val exact = score("maps", "Maps")!!
        val prefix = score("map", "Maps")!!
        val wordPrefix = score("map", "Google Maps")!!
        val initials = score("vsc", "Visual Studio Code")!!
        val substring = score("ogle", "Google")!!
        val typo = score("gooogle", "Google")!!
        val ordered = listOf(exact, prefix, wordPrefix, initials, substring, typo)
        assertEquals(ordered.sortedDescending(), ordered, "scores: $ordered")
    }

    @Test
    fun `every query word may prefix a different word in any order`() {
        assertNotNull(score("maps goo", "Google Maps"))
    }

    @Test
    fun `package names match by prefix and by segment`() {
        assertEquals(1f, score("com.android.chrome", "com.android.chrome"))
        assertNotNull(score("chrome", "com.android.chrome"))
    }

    @Test
    fun `accents are ignored on both sides`() {
        assertNotNull(score("cafe", "Café Finder"))
        assertNotNull(score("café", "Cafe Finder"))
    }

    @Test
    fun `best score picks the strongest of several texts`() {
        val best = QueryMatcher.bestScore("chrome", listOf(MatchTarget("Browser"), MatchTarget("com.android.chrome")))
        assertNotNull(best)
    }

    @Test
    fun `scores stay within zero to one`() {
        listOf("g", "gm", "gmail", "gmial", "mail").forEach { q ->
            score(q, "Gmail")?.let { assertTrue(it in 0f..1f, "$q -> $it") }
        }
    }
}
