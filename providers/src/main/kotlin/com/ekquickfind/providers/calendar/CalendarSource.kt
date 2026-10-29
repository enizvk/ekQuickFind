package com.ekquickfind.providers.calendar

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.CalendarContract.Instances

/** One occurrence of an event (a repeating event has one per date). */
data class EventRow(
    val eventId: Long,
    val title: String,
    val location: String?,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
)

/** Events in the phone's calendars; blocking, call off the main thread. */
interface CalendarSource {
    /** Occurrences between [from] and [to] whose title, place or notes contain [text]. */
    fun search(
        text: String,
        from: Long,
        to: Long,
    ): List<EventRow>

    /** Occurrences overlapping [from]..[to]. */
    fun between(
        from: Long,
        to: Long,
    ): List<EventRow>

    fun instance(
        eventId: Long,
        begin: Long,
    ): EventRow?
}

class ContentResolverCalendarSource(
    private val resolver: ContentResolver,
) : CalendarSource {
    override fun search(
        text: String,
        from: Long,
        to: Long,
    ) = query(range(Instances.CONTENT_SEARCH_URI, from, to).buildUpon().appendPath(text).build())

    override fun between(
        from: Long,
        to: Long,
    ) = query(range(Instances.CONTENT_URI, from, to))

    override fun instance(
        eventId: Long,
        begin: Long,
    ) = query(range(Instances.CONTENT_URI, begin, begin + 1), "${Instances.EVENT_ID} = ?", arrayOf(eventId.toString()))
        .firstOrNull { it.begin == begin }

    private fun range(
        base: Uri,
        from: Long,
        to: Long,
    ): Uri =
        base
            .buildUpon()
            .also {
                ContentUris.appendId(it, from)
                ContentUris.appendId(it, to)
            }.build()

    private fun query(
        uri: Uri,
        selection: String? = null,
        args: Array<String>? = null,
    ): List<EventRow> {
        // Only calendars the user shows in their calendar app.
        val visible = "${Instances.VISIBLE} = 1" + (selection?.let { " AND $it" } ?: "")
        return resolver.query(uri, PROJECTION, visible, args, "${Instances.BEGIN} ASC")?.use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        EventRow(
                            eventId = c.getLong(0),
                            title = c.getString(1)?.takeIf { it.isNotBlank() } ?: "(No title)",
                            location = c.getString(2)?.takeIf { it.isNotBlank() },
                            begin = c.getLong(3),
                            end = c.getLong(4),
                            allDay = c.getInt(5) == 1,
                        ),
                    )
                }
            }
        } ?: emptyList()
    }

    private companion object {
        val PROJECTION =
            arrayOf(Instances.EVENT_ID, Instances.TITLE, Instances.EVENT_LOCATION, Instances.BEGIN, Instances.END, Instances.ALL_DAY)
    }
}
