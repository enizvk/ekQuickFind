package com.ekquickfind.providers.weather

import android.Manifest
import com.ekquickfind.core.search.Glyph
import com.ekquickfind.core.search.ProviderId
import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.SearchProvider
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.Section
import com.ekquickfind.data.prefs.UserPreferences
import com.ekquickfind.providers.PermissionChecker
import com.ekquickfind.providers.RequestPermissionAction
import com.ekquickfind.providers.sites.KeywordQuery
import com.ekquickfind.providers.sites.SiteCatalog
import com.ekquickfind.providers.sites.SitesConfig
import com.ekquickfind.providers.web.NetworkPolicy
import com.ekquickfind.providers.web.SearchWebAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

/**
 * A live weather card for "weather sofia", "sofia weather" or "w: sofia"; "weather" alone uses the phone's
 * approximate location. Asks Open-Meteo only then, and keeps each answer for 15 minutes.
 */
class WeatherProvider(
    private val prefs: suspend () -> UserPreferences,
    private val client: WeatherClient,
    /** Approximate latitude and longitude, or null when unknown. */
    private val location: suspend () -> Pair<Double, Double>?,
    private val permissions: PermissionChecker,
    private val policy: NetworkPolicy,
    private val io: CoroutineDispatcher,
    private val locale: () -> Locale = Locale::getDefault,
    private val now: () -> Long = System::currentTimeMillis,
) : SearchProvider {
    override val id = ID
    override val section = Section.QuickAnswers

    // Network answers take longer than on-device ones.
    override val budgetMillis = 2_000L

    private val forecasts = mutableMapOf<String, Pair<Long, Forecast>>()
    private val places = mutableMapOf<String, Place?>()

    override fun search(query: Query): Flow<List<SearchResult>> =
        flow {
            val p = prefs()
            val config = SitesConfig.from(p)
            val match =
                KeywordQuery
                    .parse(query.raw, config.keys, config.separator)
                    ?.takeIf { it.siteId == SiteCatalog.WEATHER_ID }
            if (match == null) {
                emit(emptyList())
                return@flow
            }
            if (match.text.isEmpty() && !permissions.isGranted(PERMISSION)) {
                emit(listOf(askForLocation()))
                return@flow
            }
            // Typing a place name: wait for a pause before asking the network.
            delay(DEBOUNCE_MS)
            val card =
                try {
                    withContext(io) { card(match.text, p, match.byKeyword) }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
            emit(listOfNotNull(card))
        }

    private suspend fun card(
        text: String,
        prefs: UserPreferences,
        byKeyword: Boolean,
    ): SearchResult? {
        val here = text.isEmpty()
        val (name, lat, lon) =
            if (here) {
                val (la, lo) = location() ?: return null
                Triple("Here", la, lo)
            } else {
                val place = places.getOrPut(text.lowercase()) { client.place(text, locale().language) } ?: return null
                Triple(place.name, place.latitude, place.longitude)
            }
        val fahrenheit = prefs.weatherUnits.fahrenheit(locale())
        val key = "%.2f,%.2f,%b".format(Locale.ROOT, lat, lon, fahrenheit)
        val forecast =
            forecasts[key]?.takeIf { now() - it.first < CACHE_MS }?.second
                ?: client.forecast(lat, lon, fahrenheit)?.also { forecasts[key] = now() to it }
                ?: return null
        return SearchResult(
            id = ResultId("weather:$name"),
            providerId = ID,
            section = section,
            title = "${forecast.temperature.roundToInt()}° ${WeatherCodes.describe(forecast.code)} · $name",
            subtitle = "High ${forecast.high.roundToInt()}° · Low ${forecast.low.roundToInt()}° · Open-Meteo",
            icon = ResultIcon.Vector(Glyph.Weather),
            score = if (byKeyword) 1f else TRIGGER_SCORE,
            // More detail is one search away.
            primaryAction = SearchWebAction(if (here) "weather" else "weather $name", policy),
            isRecordable = false,
        )
    }

    private fun askForLocation() =
        SearchResult(
            id = ResultId("weather:location"),
            providerId = ID,
            section = section,
            title = "Allow location for local weather",
            subtitle = "Or type a place, like “weather ${SAMPLE_PLACE}”",
            icon = ResultIcon.Vector(Glyph.Weather),
            score = TRIGGER_SCORE,
            primaryAction = RequestPermissionAction(listOf(PERMISSION)),
            isRecordable = false,
        )

    companion object {
        val ID = ProviderId("weather")
        const val PERMISSION = Manifest.permission.ACCESS_COARSE_LOCATION
        private const val DEBOUNCE_MS = 250L
        private const val CACHE_MS = 15L * 60 * 1000
        private const val TRIGGER_SCORE = 0.9f
        private const val SAMPLE_PLACE = "London"
    }
}
