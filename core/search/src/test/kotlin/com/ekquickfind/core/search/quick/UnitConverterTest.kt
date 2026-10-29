package com.ekquickfind.core.search.quick

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class UnitConverterTest {
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        delimiter = '|',
        value = [
            // spec examples
            "5 km in miles | 3.11 mi",
            "72f to c | 22.2 °C",
            // length
            "1 mile in km | 1.61 km",
            "5km to m | 5000 m",
            "12 inches in cm | 30.5 cm",
            "6 ft in m | 1.83 m",
            // mass
            "100 kg in lb | 220.5 lb",
            "1 lb to g | 453.6 g",
            "16 oz in lbs | 1 lb",
            // temperature
            "100 c to f | 212 °F",
            "0 kelvin in celsius | -273.2 °C",
            "-40 f to c | -40 °C",
            // volume
            "1 gallon in liters | 3.79 L",
            "500 ml to cups | 2.11 cup",
            // speed
            "100 km/h in mph | 62.1 mph",
            "10 m/s to kmh | 36 km/h",
            // data
            "1 gb in mb | 1000 MB",
            "1 gib to mib | 1024 MiB",
            "8 bits in bytes | 1 B",
            // time
            "90 min in hours | 1.5 h",
            "2 days to hours | 48 h",
            "1 year in days | 365.2 d",
            // decimals
            "2.5 kg in lb | 5.51 lb",
        ],
    )
    fun `converts between units`(
        input: String,
        expected: String,
    ) {
        assertEquals(expected, UnitConverter.convert(input)?.formatted)
    }

    @ParameterizedTest(name = "\"{0}\" has no answer")
    @ValueSource(strings = ["5 km in kg", "5 parsecs in km", "km in miles", "5 km", "weather in london", "12*8", "5 km in"])
    fun `returns nothing for unknown or mismatched units`(input: String) {
        assertNull(UnitConverter.convert(input))
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = ["3.1068 | 3.11", "22.222 | 22.2", "220.46 | 220.5", "5000.4 | 5000", "0.004 | 0.004"])
    fun `precision depends on magnitude`(
        value: Double,
        expected: String,
    ) {
        assertEquals(expected, UnitConverter.formatMagnitude(value))
    }
}
