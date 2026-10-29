package com.ekquickfind.core.search.quick

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs

data class Conversion(
    val input: String,
    val value: Double,
    val fromSymbol: String,
    val toSymbol: String,
    val result: Double,
    val formatted: String,
) {
    val source: String get() = "${formatNumber(value, 10)} $fromSymbol"
}

/** Converts "<number> <unit> in|to|as|= <unit>" between units of the same kind. */
object UnitConverter {
    private enum class Kind { Length, Mass, Temperature, Volume, Speed, Data, Time }

    private class Unit(
        val symbol: String,
        val kind: Kind,
        /** Multiply by this to get the base unit (metre, kilogram, litre, m/s, byte, second). */
        val factor: Double = 1.0,
    )

    private val units: Map<String, Unit> =
        buildMap {
            fun add(
                unit: Unit,
                vararg aliases: String,
            ) = aliases.forEach { put(it, unit) }

            add(Unit("mm", Kind.Length, 0.001), "mm", "millimeter", "millimetre")
            add(Unit("cm", Kind.Length, 0.01), "cm", "centimeter", "centimetre")
            add(Unit("m", Kind.Length), "m", "meter", "metre")
            add(Unit("km", Kind.Length, 1000.0), "km", "kilometer", "kilometre", "kms")
            add(Unit("in", Kind.Length, 0.0254), "in", "inch", "inches", "\"")
            add(Unit("ft", Kind.Length, 0.3048), "ft", "foot", "feet", "'")
            add(Unit("yd", Kind.Length, 0.9144), "yd", "yard", "yds")
            add(Unit("mi", Kind.Length, 1609.344), "mi", "mile")
            add(Unit("nmi", Kind.Length, 1852.0), "nmi", "nautical mile")

            add(Unit("mg", Kind.Mass, 1e-6), "mg", "milligram")
            add(Unit("g", Kind.Mass, 0.001), "g", "gram", "gramme")
            add(Unit("kg", Kind.Mass), "kg", "kilogram", "kilo", "kgs")
            add(Unit("t", Kind.Mass, 1000.0), "t", "tonne", "ton", "metric ton")
            add(Unit("oz", Kind.Mass, 0.028349523125), "oz", "ounce")
            add(Unit("lb", Kind.Mass, 0.45359237), "lb", "lbs", "pound")
            add(Unit("st", Kind.Mass, 6.35029318), "st", "stone")

            add(Unit("°C", Kind.Temperature), "c", "°c", "celsius", "centigrade", "degc")
            add(Unit("°F", Kind.Temperature), "f", "°f", "fahrenheit", "degf")
            add(Unit("K", Kind.Temperature), "k", "kelvin")

            add(Unit("ml", Kind.Volume, 0.001), "ml", "milliliter", "millilitre")
            add(Unit("cl", Kind.Volume, 0.01), "cl", "centiliter", "centilitre")
            add(Unit("dl", Kind.Volume, 0.1), "dl", "deciliter", "decilitre")
            add(Unit("L", Kind.Volume), "l", "liter", "litre", "ltr")
            add(Unit("m³", Kind.Volume, 1000.0), "m3", "m³", "cubic meter", "cubic metre")
            add(Unit("gal", Kind.Volume, 3.785411784), "gal", "gallon")
            add(Unit("qt", Kind.Volume, 0.946352946), "qt", "quart")
            add(Unit("pt", Kind.Volume, 0.473176473), "pt", "pint")
            add(Unit("cup", Kind.Volume, 0.2365882365), "cup")
            add(Unit("fl oz", Kind.Volume, 0.0295735295625), "fl oz", "floz", "fluid ounce")
            add(Unit("tbsp", Kind.Volume, 0.01478676478125), "tbsp", "tablespoon")
            add(Unit("tsp", Kind.Volume, 0.00492892159375), "tsp", "teaspoon")

            add(Unit("m/s", Kind.Speed), "m/s", "mps", "meters per second", "metres per second")
            add(Unit("km/h", Kind.Speed, 1 / 3.6), "km/h", "kmh", "kph", "kmph", "kilometers per hour", "kilometres per hour")
            add(Unit("mph", Kind.Speed, 0.44704), "mph", "mi/h", "miles per hour")
            add(Unit("kn", Kind.Speed, 1852.0 / 3600), "kn", "kt", "knot")
            add(Unit("ft/s", Kind.Speed, 0.3048), "ft/s", "fps")

            add(Unit("bit", Kind.Data, 0.125), "bit")
            add(Unit("B", Kind.Data), "b", "byte")
            add(Unit("KB", Kind.Data, 1e3), "kb", "kilobyte")
            add(Unit("MB", Kind.Data, 1e6), "mb", "megabyte")
            add(Unit("GB", Kind.Data, 1e9), "gb", "gigabyte")
            add(Unit("TB", Kind.Data, 1e12), "tb", "terabyte")
            add(Unit("KiB", Kind.Data, 1024.0), "kib", "kibibyte")
            add(Unit("MiB", Kind.Data, 1048576.0), "mib", "mebibyte")
            add(Unit("GiB", Kind.Data, 1073741824.0), "gib", "gibibyte")
            add(Unit("TiB", Kind.Data, 1099511627776.0), "tib", "tebibyte")

            add(Unit("ms", Kind.Time, 0.001), "ms", "millisecond")
            add(Unit("s", Kind.Time), "s", "sec", "secs", "second")
            add(Unit("min", Kind.Time, 60.0), "min", "mins", "minute")
            add(Unit("h", Kind.Time, 3600.0), "h", "hr", "hrs", "hour")
            add(Unit("d", Kind.Time, 86400.0), "d", "day")
            add(Unit("wk", Kind.Time, 604800.0), "wk", "week")
            add(Unit("mo", Kind.Time, 2629746.0), "mo", "month")
            add(Unit("yr", Kind.Time, 31556952.0), "yr", "year")
        }

    private val pattern =
        Regex(
            "^\\s*(-?\\d+(?:\\.\\d+)?|-?\\.\\d+)\\s*(.+?)\\s+(?:in|to|as|into|=)\\s+(.+?)\\s*$",
            RegexOption.IGNORE_CASE,
        )

    fun convert(input: String): Conversion? {
        val match = pattern.matchEntire(input) ?: return null
        val (number, fromText, toText) = match.destructured
        val value = number.toDoubleOrNull() ?: return null
        val from = lookup(fromText) ?: return null
        val to = lookup(toText) ?: return null
        if (from.kind != to.kind) return null

        val result =
            if (from.kind == Kind.Temperature) {
                fromKelvin(toKelvin(value, from), to)
            } else {
                value * from.factor / to.factor
            }
        return Conversion(input.trim(), value, from.symbol, to.symbol, result, "${formatMagnitude(result)} ${to.symbol}")
    }

    /** 2 decimals below 10, 1 below 1000, whole numbers above; tiny values keep 3 significant digits. */
    fun formatMagnitude(value: Double): String {
        val size = abs(value)
        return when {
            size != 0.0 && size < 0.01 ->
                BigDecimal
                    .valueOf(value)
                    .round(MathContext(3))
                    .stripTrailingZeros()
                    .toPlainString()
            size < 10 -> formatNumber(value, 2)
            size < 1000 -> formatNumber(value, 1)
            else -> formatNumber(value, 0)
        }
    }

    private fun lookup(text: String): Unit? {
        val key =
            text
                .trim()
                .lowercase()
                .removeSuffix(".")
                .replace(Regex("\\s+"), " ")
        return units[key] ?: units[key.removeSuffix("s")]
    }

    private fun toKelvin(
        value: Double,
        unit: Unit,
    ) = when (unit.symbol) {
        "°C" -> value + 273.15
        "°F" -> (value - 32) * 5 / 9 + 273.15
        else -> value
    }

    private fun fromKelvin(
        kelvin: Double,
        unit: Unit,
    ) = when (unit.symbol) {
        "°C" -> kelvin - 273.15
        "°F" -> (kelvin - 273.15) * 9 / 5 + 32
        else -> kelvin
    }
}
