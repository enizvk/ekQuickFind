package com.ekquickfind.providers.sites

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeywordQueryTest {
    private val youtube = SiteKey("youtube", "yt", triggers = listOf("youtube"))
    private val weather = SiteKey("weather", "w", triggers = listOf("weather", "forecast"))
    private val sites = listOf(youtube, weather)

    private fun parse(
        raw: String,
        separator: String = "",
    ) = KeywordQuery.parse(raw, sites, separator)

    @Test
    fun byDefaultTheKeywordIsFollowedByASpace() {
        val expected = KeywordMatch("youtube", "cats", byKeyword = true)
        assertEquals(expected, parse("yt cats"))
        assertEquals(expected, parse("YT   cats"))
    }

    @Test
    fun aChosenSeparatorIsOptionalAndMayTouchTheKeyword() {
        val expected = KeywordMatch("youtube", "lofi beats", byKeyword = true)
        assertEquals(expected, parse("yt: lofi beats", ":"))
        assertEquals(expected, parse("yt : lofi beats", ":"))
        assertEquals(expected, parse("yt lofi beats", ":"))
        assertEquals(expected, parse("yt - lofi beats", "-"))
        assertEquals(expected, parse("yt -> lofi beats", "->"))
    }

    @Test
    fun aSpaceAfterTheSeparatorIsNeededSoNamesWithItStayNormal() {
        assertNull(parse("yt-dlp download", "-"))
        assertNull(parse("yt:cats", ":"))
    }

    @Test
    fun withTheDefaultSpaceAColonIsPartOfTheSearch() {
        assertEquals(KeywordMatch("youtube", ": cats", byKeyword = true), parse("yt : cats"))
        assertNull(parse("yt: cats"))
    }

    @Test
    fun theKeywordMustBeTheFirstWordAndHaveSomethingAfterIt() {
        assertNull(parse("cats yt"))
        assertNull(parse("yt"))
        assertNull(parse("yt:   "))
        assertNull(parse("ytcats"))
    }

    @Test
    fun aMatchingWordAtTheStartOrEndAlsoCounts() {
        assertEquals(KeywordMatch("youtube", "cats", byKeyword = false), parse("youtube cats"))
        assertEquals(KeywordMatch("weather", "Sofia", byKeyword = false), parse("Sofia weather"))
        assertEquals(KeywordMatch("weather", "london", byKeyword = false), parse("forecast london"))
    }

    @Test
    fun aMatchingWordAloneCountsWithNothingElse() {
        assertEquals(KeywordMatch("weather", "", byKeyword = false), parse("weather"))
    }

    @Test
    fun ordinarySearchesAreLeftAlone() {
        assertNull(parse("weatherproof jacket"))
        assertNull(parse("maps"))
    }

    // The keyword as it's being typed, for colouring it in the bar.

    private fun prefix(
        raw: String,
        separator: String = "",
    ) = KeywordQuery.prefix(raw, sites, separator)

    @Test
    fun theKeywordIsSpottedOnceASpaceFollows() {
        assertEquals(KeywordPrefix("youtube", end = 2), prefix("yt "))
        assertEquals(KeywordPrefix("youtube", end = 2), prefix("YT cats"))
        assertNull(prefix("yt"))
        assertNull(prefix("ytcats"))
    }

    @Test
    fun aSeparatorIsColouredWithTheKeyword() {
        assertEquals(KeywordPrefix("youtube", end = 3), prefix("yt: cats", ":"))
        assertEquals(KeywordPrefix("youtube", end = 4), prefix("yt : ", ":"))
        assertNull(prefix("yt:", ":"))
        assertNull(prefix("yt-dlp", "-"))
    }

    @Test
    fun matchingWordsAreNotColoured() {
        assertNull(prefix("youtube cats"))
        assertNull(prefix("sofia weather"))
    }
}
