package com.ekquickfind.providers.weather

import com.ekquickfind.data.prefs.WeatherUnits
import com.ekquickfind.providers.getText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import java.util.Locale

data class Place(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

data class Forecast(
    val temperature: Double,
    val code: Int,
    val high: Double,
    val low: Double,
    val unit: String,
)

/** Where weather comes from; [place] turns a typed name into coordinates. */
interface WeatherClient {
    suspend fun place(
        name: String,
        language: String,
    ): Place?

    suspend fun forecast(
        latitude: Double,
        longitude: Double,
        fahrenheit: Boolean,
    ): Forecast?
}

/** Open-Meteo: free, no account or key; its data is CC BY 4.0, so the card credits it. */
class OpenMeteoClient(
    private val http: OkHttpClient,
) : WeatherClient {
    override suspend fun place(
        name: String,
        language: String,
    ): Place? =
        OpenMeteo.parsePlace(
            http.getText(
                "https://geocoding-api.open-meteo.com/v1/search?name=${com.ekquickfind.providers.sites.encode(
                    name,
                )}&count=1&language=$language",
            ),
        )

    override suspend fun forecast(
        latitude: Double,
        longitude: Double,
        fahrenheit: Boolean,
    ): Forecast? =
        OpenMeteo.parseForecast(
            http.getText(
                "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&timezone=auto" +
                    if (fahrenheit) "&temperature_unit=fahrenheit" else "",
            ),
        )
}

object OpenMeteo {
    fun parsePlace(body: String): Place? =
        runCatching {
            val first =
                Json
                    .parseToJsonElement(body)
                    .jsonObject["results"]
                    ?.jsonArray
                    ?.firstOrNull()
                    ?.jsonObject ?: return null
            Place(first.text("name") ?: return null, first.number("latitude") ?: return null, first.number("longitude") ?: return null)
        }.getOrNull()

    fun parseForecast(body: String): Forecast? =
        runCatching {
            val root = Json.parseToJsonElement(body).jsonObject
            val current = root["current"]?.jsonObject ?: return null
            val daily = root["daily"]?.jsonObject ?: return null
            Forecast(
                temperature = current.number("temperature_2m") ?: return null,
                code = current["weather_code"]?.jsonPrimitive?.intOrNull ?: return null,
                high =
                    daily["temperature_2m_max"]
                        ?.jsonArray
                        ?.firstOrNull()
                        ?.jsonPrimitive
                        ?.doubleOrNull ?: return null,
                low =
                    daily["temperature_2m_min"]
                        ?.jsonArray
                        ?.firstOrNull()
                        ?.jsonPrimitive
                        ?.doubleOrNull ?: return null,
                unit = root["current_units"]?.jsonObject?.text("temperature_2m") ?: "°",
            )
        }.getOrNull()

    private fun JsonObject.text(key: String) = this[key]?.jsonPrimitive?.content

    private fun JsonObject.number(key: String) = this[key]?.jsonPrimitive?.doubleOrNull
}

/** WMO weather codes as Open-Meteo sends them, in plain words. */
object WeatherCodes {
    fun describe(code: Int): String =
        when (code) {
            0 -> "Clear"
            1 -> "Mostly clear"
            2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            in 51..57 -> "Drizzle"
            in 61..67 -> "Rain"
            in 71..77 -> "Snow"
            in 80..82 -> "Showers"
            85, 86 -> "Snow showers"
            in 95..99 -> "Thunderstorm"
            else -> "Weather"
        }
}

/** Auto: °F where people use it (the US and a few others), °C everywhere else. */
fun WeatherUnits.fahrenheit(locale: Locale): Boolean =
    when (this) {
        WeatherUnits.Celsius -> false
        WeatherUnits.Fahrenheit -> true
        WeatherUnits.Auto -> locale.country in FAHRENHEIT_REGIONS
    }

private val FAHRENHEIT_REGIONS = setOf("US", "LR", "MM", "BS", "BZ", "KY", "PW", "FM", "MH")
