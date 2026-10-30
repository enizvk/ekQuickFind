package com.ekquickfind.providers.weather

import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.Section
import com.ekquickfind.data.prefs.UserPreferences
import com.ekquickfind.data.prefs.WeatherUnits
import com.ekquickfind.providers.PermissionChecker
import com.ekquickfind.providers.RequestPermissionAction
import com.ekquickfind.providers.web.NetworkPolicy
import com.ekquickfind.providers.web.WebEngine
import com.ekquickfind.providers.web.WebSettingsSnapshot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class WeatherTest {
    private fun fixture(name: String) = javaClass.getResource("/weather/$name")!!.readText()

    @Test
    fun aPlaceIsReadFromTheGeocodingAnswer() {
        assertEquals(Place("Sofia", 42.69751, 23.32415), OpenMeteo.parsePlace(fixture("geocoding-sofia.json")))
        assertNull(OpenMeteo.parsePlace(fixture("geocoding-none.json")))
    }

    @Test
    fun theForecastIsReadWithItsUnit() {
        assertEquals(Forecast(14.2, 3, 17.1, 8.6, "°C"), OpenMeteo.parseForecast(fixture("forecast.json")))
        assertNull(OpenMeteo.parseForecast("not json"))
    }

    @Test
    fun weatherCodesReadLikeWords() {
        assertEquals("Clear", WeatherCodes.describe(0))
        assertEquals("Overcast", WeatherCodes.describe(3))
        assertEquals("Rain", WeatherCodes.describe(63))
        assertEquals("Thunderstorm", WeatherCodes.describe(95))
    }

    @Test
    fun autoUnitsFollowTheRegion() {
        assertTrue(WeatherUnits.Auto.fahrenheit(Locale.US))
        assertTrue(!WeatherUnits.Auto.fahrenheit(Locale.UK))
        assertTrue(WeatherUnits.Fahrenheit.fahrenheit(Locale.UK))
        assertTrue(!WeatherUnits.Celsius.fahrenheit(Locale.US))
    }

    private class FakeClient : WeatherClient {
        var forecasts = 0
        var lastFahrenheit = false

        override suspend fun place(
            name: String,
            language: String,
        ) = if (name.equals("sofia", ignoreCase = true)) Place("Sofia", 42.7, 23.3) else null

        override suspend fun forecast(
            latitude: Double,
            longitude: Double,
            fahrenheit: Boolean,
        ): Forecast {
            forecasts++
            lastFahrenheit = fahrenheit
            return Forecast(14.2, 3, 17.1, 8.6, if (fahrenheit) "°F" else "°C")
        }
    }

    private val policy =
        NetworkPolicy { WebSettingsSnapshot(true, WebEngine.forChoice(com.ekquickfind.data.prefs.SearchEngineChoice.Google, null, null)) }

    private fun TestScope.provider(
        client: WeatherClient,
        // Weather is off until turned on; these tests are about the card itself.
        prefs: UserPreferences =
            UserPreferences(
                sites =
                    mapOf(
                        "weather" to
                            com.ekquickfind.data.prefs
                                .SiteState(enabled = true),
                    ),
            ),
        located: Boolean = false,
        here: Pair<Double, Double>? = 42.7 to 23.3,
    ) = WeatherProvider(
        prefs = { prefs },
        client = client,
        location = { here },
        permissions = PermissionChecker { located },
        policy = policy,
        io = StandardTestDispatcher(testScheduler),
        locale = { Locale.UK },
        now = { 0L },
    )

    @Test
    fun aPlaceAfterTheKeywordGivesAWeatherCardOnTop() =
        runTest {
            val card = provider(FakeClient()).search(Query("w sofia")).first().single()
            assertEquals("14° Overcast · Sofia", card.title)
            assertEquals("High 17° · Low 9° · Open-Meteo", card.subtitle)
            assertEquals(Section.QuickAnswers, card.section)
        }

    @Test
    fun theWordWeatherWorksBeforeOrAfterThePlace() =
        runTest {
            assertEquals(
                "14° Overcast · Sofia",
                provider(FakeClient())
                    .search(Query("sofia weather"))
                    .first()
                    .single()
                    .title,
            )
        }

    @Test
    fun weatherAloneUsesTheLocationOrAsksForIt() =
        runTest {
            assertEquals(
                "14° Overcast · Here",
                provider(FakeClient(), located = true)
                    .search(Query("weather"))
                    .first()
                    .single()
                    .title,
            )
            val ask = provider(FakeClient(), located = false).search(Query("weather")).first().single()
            assertTrue(ask.primaryAction is RequestPermissionAction)
        }

    @Test
    fun anUnknownPlaceShowsNothing() =
        runTest {
            assertTrue(provider(FakeClient()).search(Query("weather atlantis")).first().isEmpty())
        }

    @Test
    fun aRepeatWithinFifteenMinutesUsesTheSavedForecast() =
        runTest {
            val client = FakeClient()
            val p = provider(client)
            p.search(Query("weather sofia")).first()
            p.search(Query("w sofia")).first()
            assertEquals(1, client.forecasts)
        }

    @Test
    fun fahrenheitIsAskedForWhenChosen() =
        runTest {
            val client = FakeClient()
            provider(
                client,
                UserPreferences(
                    weatherUnits = WeatherUnits.Fahrenheit,
                    sites =
                        mapOf(
                            "weather" to
                                com.ekquickfind.data.prefs
                                    .SiteState(enabled = true),
                        ),
                ),
            ).search(Query("weather sofia")).first()
            assertTrue(client.lastFahrenheit)
        }

    @Test
    fun turnedOffWeatherStaysQuiet() =
        runTest {
            val prefs =
                UserPreferences(
                    sites =
                        mapOf(
                            "weather" to
                                com.ekquickfind.data.prefs
                                    .SiteState(enabled = false),
                        ),
                )
            assertTrue(provider(FakeClient(), prefs).search(Query("weather sofia")).first().isEmpty())
        }
}
