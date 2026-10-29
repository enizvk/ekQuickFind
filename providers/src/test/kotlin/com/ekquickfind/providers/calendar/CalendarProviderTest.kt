package com.ekquickfind.providers.calendar

import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.providers.PermissionChecker
import com.ekquickfind.providers.RequestPermissionAction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale

class CalendarProviderTest {
    private val utc = ZoneOffset.UTC

    // Monday 2 November 2026, 09:00.
    private val now = at(2, 9)

    private fun at(
        day: Int,
        hour: Int,
        minute: Int = 0,
    ) = ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, utc).toInstant().toEpochMilli()

    private val hour = 60L * 60 * 1000

    private fun event(
        id: Long,
        title: String,
        begin: Long,
        location: String? = null,
        allDay: Boolean = false,
    ) = EventRow(id, title, location, begin, begin + hour, allDay)

    private class FakeCalendar(
        val events: List<EventRow>,
    ) : CalendarSource {
        override fun search(
            text: String,
            from: Long,
            to: Long,
        ) = events.filter {
            it.begin in from..to &&
                (text.lowercase() in it.title.lowercase() || text.lowercase() in it.location.orEmpty().lowercase())
        }

        override fun between(
            from: Long,
            to: Long,
        ) = events.filter { it.end > from && it.begin < to }

        override fun instance(
            eventId: Long,
            begin: Long,
        ) = events.firstOrNull { it.eventId == eventId && it.begin == begin }
    }

    private fun TestScope.provider(
        events: List<EventRow>,
        granted: Boolean = true,
    ) = CalendarProvider(
        FakeCalendar(events),
        PermissionChecker { granted },
        StandardTestDispatcher(testScheduler),
        now = { now },
        zone = utc,
        locale = Locale.UK,
    )

    @Test
    fun withoutPermissionItOffersToEnableCalendarSearch() =
        runTest {
            val rows = provider(emptyList(), granted = false).search(Query("dentist")).first()
            assertEquals(listOf("Enable calendar search"), rows.map { it.title })
            assertTrue(rows.single().primaryAction is RequestPermissionAction)
        }

    @Test
    fun findsEventsByTitleOrPlaceWithUpcomingOnesFirst() =
        runTest {
            val past = event(1, "Dentist", at(1, 10) - 7 * 24 * hour)
            val soon = event(2, "Dentist", at(3, 14))
            val meeting = event(3, "Check-up", at(4, 8), location = "Dentist clinic")
            val titles = provider(listOf(past, soon, meeting)).search(Query("dentist")).first().sortedByDescending { it.score }
            assertEquals(
                listOf(soon.eventId, meeting.eventId, past.eventId),
                titles.map {
                    it.id.value
                        .split(":")[1]
                        .toLong()
                },
            )
        }

    @Test
    fun anEventShowsItsDayTimeAndPlace() =
        runTest {
            val row = provider(listOf(event(2, "Dentist", at(3, 14), location = "Clinic"))).search(Query("dentist")).first().single()
            assertEquals("Tue 3 Nov · 14:00 · Clinic", row.subtitle)
        }

    @Test
    fun allDayEventsSayAllDay() =
        runTest {
            val row = provider(listOf(event(5, "Holiday", at(6, 0), allDay = true))).search(Query("holiday")).first().single()
            assertEquals("Fri 6 Nov · All day", row.subtitle)
        }

    @Test
    fun nextUpIsTheNextTimedEventInTheComingTwelveHours() =
        runTest {
            val events =
                listOf(
                    event(1, "Stand-up", at(2, 10)),
                    event(2, "Lunch", at(2, 12)),
                    event(3, "Tomorrow", at(3, 9)),
                    event(4, "Holiday", at(2, 0), allDay = true),
                )
            assertEquals("Stand-up", provider(events).nextUp()?.title)
        }

    @Test
    fun aMeetingRunningNowIsNextUp() =
        runTest {
            assertEquals("Review", provider(listOf(event(1, "Review", at(2, 8, 30)))).nextUp()?.title)
        }

    @Test
    fun nothingWithinTwelveHoursMeansNoNextUp() =
        runTest {
            assertNull(provider(listOf(event(1, "Later", at(3, 9)))).nextUp())
            assertNull(provider(listOf(event(1, "Stand-up", at(2, 10))), granted = false).nextUp())
        }

    @Test
    fun aSavedEventIsRebuiltFromItsId() =
        runTest {
            val p = provider(listOf(event(7, "Dentist", at(3, 14))))
            val id =
                p
                    .search(Query("dentist"))
                    .first()
                    .single()
                    .id
            assertEquals("Dentist", p.resolve(ResultId(id.value))?.title)
        }
}
