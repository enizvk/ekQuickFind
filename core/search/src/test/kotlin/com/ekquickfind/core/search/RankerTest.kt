package com.ekquickfind.core.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class RankerTest {
    private val now = 1_000 * DAY_MS
    private val ranker = Ranker()

    private fun prefixScore(
        query: String,
        title: String,
    ) = QueryMatcher.score(query, MatchTarget(title))!!

    @Test
    fun `an app opened five times from search ranks above untouched apps with the same prefix`() {
        val settings = result("Settings", prefixScore("s", "Settings"))
        val signal = result("Signal", prefixScore("s", "Signal"))
        val spotify = result("Spotify", prefixScore("s", "Spotify"))
        val signals = RankingSignals(launches = mapOf(spotify.id to LaunchStats(count = 5, lastLaunchedAt = now - DAY_MS)), now = now)

        val ranked = ranker.rank(listOf(settings, signal, spotify), signals).map { it.result.title }

        assertEquals("Spotify", ranked.first())
    }

    @Test
    fun `pinned results come first among equal text matches`() {
        val camera = result("Camera", 0.9f)
        val calendar = result("Calendar", 0.9f)
        val signals = RankingSignals(pinned = setOf(calendar.id), now = now)

        assertEquals(listOf("Calendar", "Camera"), ranker.rank(listOf(camera, calendar), signals).map { it.result.title })
    }

    @ParameterizedTest(name = "{0} launches, last {1} days ago -> boost between {2} and {3}")
    @CsvSource(
        "0, 0, 0.0, 0.0",
        "1, 0, 0.15, 0.2",
        "5, 0, 0.4, 0.5",
        "5, 30, 0.2, 0.25",
        "5, 90, 0.05, 0.06",
        "50, 0, 0.9, 1.0",
    )
    fun `launch boost grows with count and decays with age`(
        count: Int,
        daysAgo: Int,
        min: Float,
        max: Float,
    ) {
        val boost = ranker.launchBoost(LaunchStats(count, now - daysAgo * DAY_MS), now)
        assertTrue(boost in min..max, "boost=$boost")
    }

    @Test
    fun `section weight lets a quick answer beat an exact app match`() {
        val answer = result("4", 1f, Section.QuickAnswers)
        val app = result("2+2 Game", 1f, Section.Apps)
        assertEquals(
            "4",
            ranker
                .rank(listOf(app, answer), RankingSignals(now = now))
                .first()
                .result.title,
        )
    }

    @Test
    fun `fallback and non-recordable rows get no history boost`() {
        val fallback = result("Search the web", 0.1f, Section.Web, isFallback = true)
        val enable = result("Enable contacts", 0.1f, Section.Contacts, isRecordable = false)
        val signals =
            RankingSignals(
                launches = mapOf(fallback.id to LaunchStats(50, now), enable.id to LaunchStats(50, now)),
                pinned = setOf(fallback.id, enable.id),
                now = now,
            )
        ranker.rank(listOf(fallback, enable), signals).forEach {
            assertEquals(it.result.score * it.result.section.weight, it.score, 1e-6f)
        }
    }

    @Test
    fun `equal scores keep provider order`() {
        val items = listOf(result("B", 0.5f), result("A", 0.5f), result("C", 0.5f))
        assertEquals(listOf("B", "A", "C"), ranker.rank(items, RankingSignals(now = now)).map { it.result.title })
    }
}
