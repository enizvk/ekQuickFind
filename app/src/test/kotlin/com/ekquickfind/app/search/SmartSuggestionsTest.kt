package com.ekquickfind.app.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartSuggestionsTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 100 * day

    private fun launch(
        id: String,
        hour: Int,
        daysAgo: Int = 1,
        weekend: Boolean = false,
        device: String? = null,
    ) = PastLaunch("apps", id, now - daysAgo * day, LaunchContext(hour, weekend, device))

    private fun rank(
        launches: List<PastLaunch>,
        context: LaunchContext,
        limit: Int = 5,
    ) = SmartSuggestions.rank(launches, context, now, limit).map { it.resultId }

    @Test
    fun suggestsWhatYouUsuallyOpenAtThisHour() {
        val launches = (1..3).flatMap { listOf(launch("news", 8, it), launch("tv", 20, it)) }
        assertEquals(listOf("news"), rank(launches, LaunchContext(8, weekend = false, audioDevice = null)))
    }

    @Test
    fun anHourEitherSideStillCountsEvenAcrossMidnight() {
        val launches = (1..3).map { launch("alarm", 23, it) }
        assertEquals(listOf("alarm"), rank(launches, LaunchContext(0, weekend = false, audioDevice = null)))
    }

    @Test
    fun aConnectedDeviceBringsUpWhatYouUseWithIt() {
        val launches =
            listOf(launch("music", 17, 2, device = "My Car"), launch("music", 9, 3, device = "My Car")) +
                (1..3).map { launch("maps", 13, it) }
        assertEquals("music", rank(launches, LaunchContext(13, weekend = false, audioDevice = "My Car")).first())
    }

    @Test
    fun somethingUsedOnlyWithADeviceStaysQuietWithoutIt() {
        val launches = (1..3).map { launch("podcasts", 8, it, device = "Buds") }
        assertTrue(rank(launches, LaunchContext(8, weekend = false, audioDevice = null)).isEmpty())
    }

    @Test
    fun launchesOlderThanAMonthAreIgnored() {
        val launches = (40..45).map { launch("old", 8, it) }
        assertTrue(rank(launches, LaunchContext(8, weekend = false, audioDevice = null)).isEmpty())
    }

    @Test
    fun oneOffLaunchesAreNotEnough() {
        assertTrue(rank(listOf(launch("once", 8)), LaunchContext(8, weekend = false, audioDevice = null)).isEmpty())
    }

    @Test
    fun keepsToTheLimitBestFirst() {
        val launches = (1..4).map { launch("a", 8, it) } + (1..3).map { launch("b", 8, it) } + (1..2).map { launch("c", 8, it) }
        assertEquals(listOf("a", "b"), rank(launches, LaunchContext(8, weekend = false, audioDevice = null), limit = 2))
    }

    @Test
    fun theHourAndKindOfDayComeFromTheLaunchTime() {
        val utc = java.time.ZoneOffset.UTC
        // 2026-10-31 is a Saturday.
        val saturdayNine =
            java.time.ZonedDateTime
                .of(2026, 10, 31, 9, 15, 0, 0, utc)
                .toInstant()
                .toEpochMilli()
        assertEquals(LaunchContext(9, weekend = true, audioDevice = "Buds"), LaunchContext.at(saturdayNine, "Buds", utc))
        val mondayTwentyThree =
            java.time.ZonedDateTime
                .of(2026, 11, 2, 23, 59, 0, 0, utc)
                .toInstant()
                .toEpochMilli()
        assertEquals(LaunchContext(23, weekend = false, audioDevice = null), LaunchContext.at(mondayTwentyThree, null, utc))
    }
}
