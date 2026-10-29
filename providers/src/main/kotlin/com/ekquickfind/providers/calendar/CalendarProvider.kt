package com.ekquickfind.providers.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.provider.CalendarContract
import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.ActionHost
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.Glyph
import com.ekquickfind.core.search.MatchTarget
import com.ekquickfind.core.search.ProviderId
import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.QueryMatcher
import com.ekquickfind.core.search.Requirement
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.ResultResolver
import com.ekquickfind.core.search.SearchProvider
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.Section
import com.ekquickfind.core.search.TextNormalizer
import com.ekquickfind.providers.PermissionChecker
import com.ekquickfind.providers.RequestPermissionAction
import com.ekquickfind.providers.android
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Calendar events by title or place, upcoming first; also the "Next up" event shown before anything is typed. */
class CalendarProvider(
    private val source: CalendarSource,
    private val permissions: PermissionChecker,
    private val io: CoroutineDispatcher,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
) : SearchProvider,
    ResultResolver {
    override val id = ID
    override val section = Section.Calendar
    override val requirements = setOf(Requirement.Permission(PERMISSION))

    private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM", locale)
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", locale)

    override fun search(query: Query): Flow<List<SearchResult>> =
        flow {
            if (!permissions.isGranted(PERMISSION)) {
                emit(listOf(enableRow()))
                return@flow
            }
            // Short pause so fast typing doesn't query the calendar on every character.
            delay(DEBOUNCE_MS)
            val text = query.raw.trim()
            if (text.length < MIN_LENGTH) {
                emit(emptyList())
                return@flow
            }
            val t = now()
            val rows = withContext(io) { source.search(text, t - PAST_MS, t + FUTURE_MS) }
            emit(rows.mapNotNull { toResult(it, query.normalized, query.exact, t) })
        }

    override suspend fun resolve(id: ResultId): SearchResult? {
        if (!permissions.isGranted(PERMISSION)) return null
        val (eventId, begin) =
            id.value
                .removePrefix("calendar:")
                .split(':')
                .mapNotNull { it.toLongOrNull() }
                .takeIf { it.size == 2 } ?: return null
        val row = withContext(io) { source.instance(eventId, begin) } ?: return null
        return toResult(row, TextNormalizer.normalize(row.title), exact = false, t = now())
    }

    /** The timed event running now or starting in the next 12 hours; all-day events don't count. */
    suspend fun nextUp(): SearchResult? {
        if (!permissions.isGranted(PERMISSION)) return null
        val t = now()
        val next =
            withContext(io) { source.between(t, t + NEXT_UP_MS) }
                .filter { !it.allDay && it.end > t }
                .minByOrNull { it.begin } ?: return null
        return result(next, score = 1f, subtitle = nextUpLine(next, t))
    }

    private fun toResult(
        row: EventRow,
        q: String,
        exact: Boolean,
        t: Long,
    ): SearchResult? {
        val titleScore = QueryMatcher.score(q, MatchTarget(row.title), exact)
        val placeScore = row.location?.let { QueryMatcher.score(q, MatchTarget(it), exact) }?.times(PLACE_WEIGHT)
        val match = listOfNotNull(titleScore, placeScore).maxOrNull() ?: return null
        val timeWeight =
            when {
                row.end < t -> PAST_WEIGHT
                row.begin - t <= WEEK_MS -> 1f
                else -> LATER_WEIGHT
            }
        return result(row, match * timeWeight, line(row))
    }

    private fun result(
        row: EventRow,
        score: Float,
        subtitle: String,
    ) = SearchResult(
        id = ResultId("calendar:${row.eventId}:${row.begin}"),
        providerId = ID,
        section = section,
        title = row.title,
        subtitle = subtitle,
        icon = ResultIcon.Vector(Glyph.Calendar),
        score = score,
        primaryAction = OpenEventAction(row.eventId, row.begin, row.end),
    )

    /** "Tue 3 Nov · 14:00 · Clinic"; all-day events are stored at UTC midnight, so their day is read in UTC. */
    private fun line(row: EventRow): String {
        val day = dayFormat.format(Instant.ofEpochMilli(row.begin).atZone(if (row.allDay) ZoneOffset.UTC else zone))
        val time = if (row.allDay) "All day" else timeFormat.format(Instant.ofEpochMilli(row.begin).atZone(zone))
        return listOfNotNull(day, time, row.location).joinToString(" · ")
    }

    private fun nextUpLine(
        row: EventRow,
        t: Long,
    ): String {
        val start = Instant.ofEpochMilli(row.begin).atZone(zone)
        val `when` =
            when {
                row.begin <= t -> "Now · until ${timeFormat.format(Instant.ofEpochMilli(row.end).atZone(zone))}"
                start.toLocalDate() == Instant.ofEpochMilli(t).atZone(zone).toLocalDate() -> "Today · ${timeFormat.format(start)}"
                else -> "Tomorrow · ${timeFormat.format(start)}"
            }
        return listOfNotNull(`when`, row.location).joinToString(" · ")
    }

    private fun enableRow() =
        SearchResult(
            id = ResultId("calendar:enable"),
            providerId = ID,
            section = section,
            title = "Enable calendar search",
            subtitle = "Find events and see what's next",
            icon = ResultIcon.Vector(Glyph.Lock),
            score = 0.2f,
            primaryAction = RequestPermissionAction(listOf(PERMISSION)),
            isRecordable = false,
        )

    companion object {
        val ID = ProviderId("calendar")
        const val PERMISSION = Manifest.permission.READ_CALENDAR
        private const val DEBOUNCE_MS = 80L
        private const val MIN_LENGTH = 2
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val WEEK_MS = 7 * DAY_MS
        private const val PAST_MS = 30 * DAY_MS
        private const val FUTURE_MS = 365 * DAY_MS
        private const val NEXT_UP_MS = 12L * 60 * 60 * 1000

        // Upcoming soon ranks first, then later ones, then past ones; a match on the place counts a little less.
        private const val LATER_WEIGHT = 0.95f
        private const val PAST_WEIGHT = 0.75f
        private const val PLACE_WEIGHT = 0.9f
    }
}

/** Opens the event at this occurrence in the calendar app. */
data class OpenEventAction(
    val eventId: Long,
    val begin: Long,
    val end: Long,
    override val label: String = "Open",
) : Action {
    override val glyph = Glyph.Calendar

    override suspend fun perform(host: ActionHost): ActionOutcome {
        val intent =
            Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId))
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
        return if (host.android().startActivity(intent)) ActionOutcome.Close else ActionOutcome.Stay
    }
}
