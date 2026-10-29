package com.ekquickfind.app.search

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.min

/** When and with what a result was opened; [audioDevice] is the connected headphones or car, null for the phone speaker. */
data class LaunchContext(
    val hour: Int,
    val weekend: Boolean,
    val audioDevice: String?,
) {
    companion object {
        /** The moment [millis] in the phone's time zone; the hour and weekday aren't stored, they come from the time. */
        fun at(
            millis: Long,
            audioDevice: String?,
            zone: ZoneId = ZoneId.systemDefault(),
        ): LaunchContext {
            val time = Instant.ofEpochMilli(millis).atZone(zone)
            return LaunchContext(time.hour, time.dayOfWeek == DayOfWeek.SATURDAY || time.dayOfWeek == DayOfWeek.SUNDAY, audioDevice)
        }
    }
}

data class PastLaunch(
    val providerId: String,
    val resultId: String,
    val at: Long,
    val context: LaunchContext,
)

data class Suggestion(
    val providerId: String,
    val resultId: String,
)

/**
 * Picks what you usually open in a moment like this one, from your own launch history only:
 * the same time of day (and kind of day), and whatever you use with the headphones or car that are connected now.
 */
object SmartSuggestions {
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val MAX_AGE_DAYS = 30
    private const val FULL_WEIGHT_DAYS = 7
    private const val OLDEST_WEIGHT = 0.5f

    // A device in common counts more than the clock: the car playlist is wanted at any hour in the car.
    private const val SAME_DEVICE_BONUS = 2f
    private const val OTHER_DEVICE_FACTOR = 0.3f
    private const val OTHER_DAY_FACTOR = 0.5f

    // Roughly two matching launches; one-offs aren't a habit.
    private const val MIN_SCORE = 1.5f

    fun rank(
        launches: List<PastLaunch>,
        now: LaunchContext,
        nowMillis: Long,
        limit: Int,
    ): List<Suggestion> =
        launches
            .filter { nowMillis - it.at < MAX_AGE_DAYS * DAY_MS }
            .groupBy { Suggestion(it.providerId, it.resultId) }
            .mapValues { (_, own) -> own.sumOf { weight(it, now, nowMillis).toDouble() }.toFloat() }
            .filterValues { it >= MIN_SCORE }
            .entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }

    private fun weight(
        launch: PastLaunch,
        now: LaunchContext,
        nowMillis: Long,
    ): Float {
        val then = launch.context
        val time = hourMatch(then.hour, now.hour) * if (then.weekend == now.weekend) 1f else OTHER_DAY_FACTOR
        val device =
            when {
                then.audioDevice == null -> time
                then.audioDevice == now.audioDevice -> time + SAME_DEVICE_BONUS
                // Used with headphones that aren't connected now: probably not wanted.
                else -> time * OTHER_DEVICE_FACTOR
            }
        return device * recency(nowMillis - launch.at)
    }

    /** 1 at the same hour, less an hour either side, nothing further away; the clock wraps at midnight. */
    private fun hourMatch(
        a: Int,
        b: Int,
    ): Float {
        val distance = abs(a - b).let { min(it, 24 - it) }
        return when (distance) {
            0 -> 1f
            1 -> 0.6f
            2 -> 0.25f
            else -> 0f
        }
    }

    /** Full weight for the last week, fading to half at a month. */
    private fun recency(ageMs: Long): Float {
        val days = ageMs.toFloat() / DAY_MS
        if (days <= FULL_WEIGHT_DAYS) return 1f
        val fade = (days - FULL_WEIGHT_DAYS) / (MAX_AGE_DAYS - FULL_WEIGHT_DAYS)
        return 1f - (1f - OLDEST_WEIGHT) * fade.coerceIn(0f, 1f)
    }
}
